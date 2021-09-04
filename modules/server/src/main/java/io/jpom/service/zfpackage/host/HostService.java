package io.jpom.service.zfpackage.host;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.*;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.PackageHostModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeService;
import io.jpom.system.ConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.ServerConfigBean;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


@Service
public class HostService extends BaseOperService<PackageHostModel> implements BaseDynamicService {

	@Resource
	private NodeService nodeService;

	public HostService() {
		super(ServerConfigBean.PKG_HOST_LIST);
	}

	@Override
	public void addItem(PackageHostModel packageHostModel) {
		packageHostModel.setId(IdUtil.fastSimpleUUID());
		super.addItem(packageHostModel);
	}

	@Override
	public JSONArray listToArray(String dataId) {
		return (JSONArray) JSONArray.toJSON(this.list());
	}

	@Override
	public List<PackageHostModel> list() {
		return (List<PackageHostModel>) filter(super.list(), ClassFeature.SSH);
	}

	public JSONArray listSelect(String nodeId) {
		// 查询host
		List<PackageHostModel> packageHostModels = list();
		List<NodeModel> list = nodeService.list();
		JSONArray sshList = new JSONArray();
		if (packageHostModels == null) {
			return sshList;
		}
		packageHostModels.forEach(PackageHostModel -> {
			String PackageHostModelId = PackageHostModel.getId();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", PackageHostModelId);
			jsonObject.put("name", PackageHostModel.getName());
			if (list != null) {
				for (NodeModel nodeModel : list) {
					if (!StrUtil.equals(nodeId, nodeModel.getId()) && StrUtil.equals(PackageHostModelId, nodeModel.getSshId())) {
						jsonObject.put("disabled", true);
						break;
					}
				}
			}
			sshList.add(jsonObject);
		});
		return sshList;
	}

	public static Session getSession(PackageHostModel packageHostModel) {
		Session session;
		if (packageHostModel.getConnectType() == PackageHostModel.ConnectType.PASS) {
			session = JschUtil.openSession(packageHostModel.getHost(), packageHostModel.getPort(), packageHostModel.getUser(), packageHostModel.getPassword());

		} else if (packageHostModel.getConnectType() == PackageHostModel.ConnectType.PUBKEY) {
			File tempPath = ServerConfigBean.getInstance().getTempPath();
			String sshFile = StrUtil.emptyToDefault(packageHostModel.getId(), IdUtil.fastSimpleUUID());
			File ssh = FileUtil.file(tempPath, "ssh", sshFile);
			FileUtil.writeString(packageHostModel.getPrivateKey(), ssh, CharsetUtil.UTF_8);
			byte[] pas = null;
			if (StrUtil.isNotEmpty(packageHostModel.getPassword())) {
				pas = packageHostModel.getPassword().getBytes();
			}
			session = JschUtil.openSession(packageHostModel.getHost(), packageHostModel.getPort(), packageHostModel.getUser(), FileUtil.getAbsolutePath(ssh), pas);
		} else {
			throw new IllegalArgumentException("不支持的模式");
		}
		try {
			session.setServerAliveInterval((int) TimeUnit.SECONDS.toMillis(5));
			session.setServerAliveCountMax(5);
		} catch (JSchException ignored) {
		}
		return session;

	}

	/**
	 * 检查是否存在正在运行的进程
	 *
	 * @param packageHostModel ssh
	 * @param tag      标识
	 * @return true 存在运行中的
	 * @throws IOException   IO
	 * @throws JSchException jsch
	 */
	public boolean checkSshRun(PackageHostModel packageHostModel, String tag) throws IOException, JSchException {
		String ps = StrUtil.format("ps -ef | grep -v 'grep' | egrep {}", tag);
		Session session = null;
		ChannelExec channel = null;
		try {
			session = getSession(packageHostModel);
			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			channel.setCommand(ps);
			InputStream inputStream = channel.getInputStream();
			InputStream errStream = channel.getErrStream();
			channel.connect();
			Charset charset = packageHostModel.getCharsetT();
			// 运行中
			AtomicBoolean run = new AtomicBoolean(false);
			IoUtil.readLines(inputStream, charset, (LineHandler) s -> {
				run.set(true);
			});
			if (run.get()) {
				return true;
			}
			run.set(false);
			AtomicReference<String> error = new AtomicReference<>();
			IoUtil.readLines(errStream, charset, (LineHandler) s -> {
				run.set(true);
				error.set(s);
			});
			if (run.get()) {
				throw new JpomRuntimeException("检查异常:" + error.get());
			}
		} finally {
			JschUtil.close(channel);
			JschUtil.close(session);
		}
		return false;
	}

	public String exec(PackageHostModel packageHostModel, String... command) throws IOException {
		if (ArrayUtil.isEmpty(command)) {
			return "没有任何命令";
		}
		Session session = null;
		InputStream sshExecTemplateInputStream = null;
		Sftp sftp = null;
		try {
			File buildSsh = FileUtil.file(ConfigBean.getInstance().getTempPath(), "build_ssh", packageHostModel.getId() + ".sh");
			sshExecTemplateInputStream = ResourceUtil.getStream("classpath:/bin/sshExecTemplate.sh");
			String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
			StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
			for (String s : command) {
				stringBuilder.append(s).append(StrUtil.LF);
			}
			Charset charset = packageHostModel.getCharsetT();
			FileUtil.writeString(stringBuilder.toString(), buildSsh, charset);
			//
			session = getSession(packageHostModel);
			// 上传文件
			sftp = new Sftp(session);
			String home = sftp.home();
			String path = home + "/.jpom/";
			String destFile = path + IdUtil.fastSimpleUUID() + ".sh";
			sftp.mkDirs(path);
			sftp.upload(destFile, buildSsh);

			// 执行命令
			String exec, error;
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				exec = JschUtil.exec(session, "sh " + destFile, charset, stream);
				error = new String(stream.toByteArray(), charset);
				if (StrUtil.isNotEmpty(error)) {
					error = " 错误：" + error;
				}
			} finally {
				try {
					sftp.delFile(destFile);
				} catch (Exception ignored) {
				}
			}
			return exec + error;
		} finally {
			IoUtil.close(sftp);
			IoUtil.close(sshExecTemplateInputStream);
			JschUtil.close(session);
		}
	}

	/**
	 * 执行命令
	 *
	 * @param packageHostModel ssh
	 * @param command  命令
	 * @return 结果
	 * @throws IOException   io
	 * @throws JSchException jsch
	 */
	public String exec(PackageHostModel packageHostModel, String command) throws IOException, JSchException {
		Session session = null;
		try {
			session = getSession(packageHostModel);
			return exec(session, packageHostModel.getCharsetT(), command);
		} finally {
			JschUtil.close(session);
		}
	}

	private String exec(Session session, Charset charset, String command) throws IOException, JSchException {
		ChannelExec channel = null;
		try {
			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			// 添加环境变量
			channel.setCommand(ServerExtConfigBean.getInstance().getSshInitEnv() + " && " + command);
			InputStream inputStream = channel.getInputStream();
			InputStream errStream = channel.getErrStream();
			channel.connect();
			// 读取结果
			String result = IoUtil.read(inputStream, charset);
			//
			String error = IoUtil.read(errStream, charset);
			return result + error;
		} finally {
			JschUtil.close(channel);
		}
	}

	/**
	 * 上传文件
	 *
	 * @param packageHostModel   ssh
	 * @param remotePath 远程路径
	 * @param desc       文件夹或者文件
	 */
	public void uploadDir(PackageHostModel packageHostModel, String remotePath, File desc) {
		Session session = null;
		ChannelSftp channel = null;
		try {
			session = getSession(packageHostModel);
			channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
			Sftp sftp = new Sftp(channel, packageHostModel.getCharsetT());
			sftp.syncUpload(desc, remotePath);
			//uploadDir(channel, remotePath, desc, PackageHostModel.getCharsetT());
		} finally {
			JschUtil.close(channel);
			JschUtil.close(session);
		}
	}

//	private void uploadDir(ChannelSftp channel, String remotePath, File file, Charset charset) throws FileNotFoundException, SftpException {
//		if (file.isDirectory()) {
//			File[] files = file.listFiles();
//			if (files == null || files.length <= 0) {
//				return;
//			}
//			for (File f : files) {
//				if (f.isDirectory()) {
//					String mkdir = FileUtil.normalize(remotePath + "/" + f.getName());
//					this.uploadDir(channel, mkdir, f, charset);
//				} else {
//					this.uploadDir(channel, remotePath, f, charset);
//				}
//			}
//		} else {
//			mkdir(channel, remotePath, charset);
//			String name = file.getName();
//			channel.put(new FileInputStream(file), name);
//		}
//	}
//
//	private void mkdir(ChannelSftp channel, String remotePath, Charset charset) {
//		Sftp sftp = new Sftp(channel, charset);
//		sftp.mkDirs(remotePath);
////        try {
////            channel.mkdir(remotePath);
////        } catch (SftpException ignored) {
////        }
//		try {
//			channel.cd(remotePath);
//		} catch (SftpException e) {
//			throw new RuntimeException("切换目录失败：" + remotePath, e);
//		}
//	}

	/**
	 * 下载文件
	 *
	 * @param packageHostModel   实体
	 * @param remoteFile 远程文件
	 * @param save       文件对象
	 * @throws FileNotFoundException io
	 * @throws SftpException         sftp
	 */
	public void download(PackageHostModel packageHostModel, String remoteFile, File save) throws FileNotFoundException, SftpException {
		Session session = null;
		ChannelSftp channel = null;
		OutputStream output = null;
		try {
			session = getSession(packageHostModel);
			channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
			output = new FileOutputStream(save);
			channel.get(remoteFile, output);
		} finally {
			IoUtil.close(output);
			JschUtil.close(channel);
			JschUtil.close(session);
		}
	}
}
