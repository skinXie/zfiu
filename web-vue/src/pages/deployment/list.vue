<template>
  <div >
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 1110, y: tableHeight - 60}"
      :pagination="pagination" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="packageName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

			<a-tooltip slot="packageFile" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="packageVersion" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="packageMd5" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="packageSize" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="modifyTime" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-button type="primary"  @click="handleEdit(record)">编辑</a-button>
				<a-button type="primary"  @click="handleDownLoad(record)">下载</a-button>
				<a-button type="danger"   @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editPackageVisible" width="600px" title="编辑应用" @ok="handleEditPkgOk" :maskClosable="false">
      <a-form-model ref="editPkgForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">

				<!-- 名称 -->
				<a-form-model-item label="名称" prop="packageName">
          <a-input v-model="temp.packageName" placeholder="应用名称"/>
        </a-form-model-item>

				<!-- 版本号 -->
				<a-form-model-item label="版本" prop="packageVersion">
					<a-input v-model="temp.packageVersion" placeholder="应用版本号"/>
				</a-form-model-item>

				<!-- 上传应用 -->
				<a-form-model-item label="应用" prop="installPackage">
					<a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" multiple>
						<a-button><a-icon type="upload" />选择文件</a-button>
					</a-upload>
					<a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
				</a-form-model-item>

				<!-- 绑定脚本 -->
<!--				<a-form-model-item label="触发脚本 " prop="sshId"  class="jpom-notify">-->
<!--					<a-select v-model="temp.scriptId" placeholder="后置触发脚本">-->
<!--						<a-select-option v-for="installScript in scriptList" :key="installScript.id" :disabled="installScript.disabled">{{ installScript.name }}</a-select-option>-->
<!--					</a-select>-->
<!--				</a-form-model-item>-->
      </a-form-model>


    </a-modal>

	</div>
</template>
<script>
import { mapGetters } from 'vuex';
import { parseTime } from '../../utils/time';
import {uploadFile,editPkg,getPackageList,deletePackage,downloadFile} from "@/api/install-package";
export default {
  data() {
    return {
      loading: false,
      tableHeight: '70vh',
			// 文件是否成功上传标识
			uploadSuccess: false,
			// 应用列表
      list: [],
			// 文件上传列表
			uploadFileList: [],
			// 脚本下拉列表
			scriptList: [],

      temp: {},
      editPackageVisible: false,
      columns: [
        {title: '名称', dataIndex: 'packageName', ellipsis: true, scopedSlots: {customRender: 'packageName'}, width: 120},
        {title: '应用', dataIndex: 'packageFile', ellipsis: true, scopedSlots: {customRender: 'packageFile'}, width: 200},
        {title: '版本号', dataIndex: 'packageVersion', ellipsis: true, scopedSlots: {customRender: 'packageVersion'}, width: 120},
        {title: 'MD5', dataIndex: 'packageMd5', ellipsis: true, scopedSlots: {customRender: 'packageMd5'}, width: 150},
        {title: '文件大小', dataIndex: 'packageSize', ellipsis: true, scopedSlots: {customRender: 'packageSize'}, width: 120},
				// {title: '脚本', dataIndex: 'installScript', ellipsis: true, scopedSlots: {customRender: 'installScript'}, width: 80},
				{title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          if (!text || text === '0') {
            return '';
          }
          return parseTime(text);
        }, width: 180},
        {title: '操作', dataIndex: 'operation', ellipsis: true, scopedSlots: {customRender: 'operation'}, width: 275	}
      ],
      rules: {
				packageName: [
          { required: true, message: 'Please input package name', trigger: 'blur' }
        ],
				packageVersion: [
					{ required: true, message: 'Please input package version', trigger: 'blur' }
				]
      }
    }
  },
  computed: {
    ...mapGetters([
      'getGuideFlag'
    ])
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    }
  },
  created() {
    this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs().setOptions({
          hidePrev: true,
          steps: [{
            title: '导航小助手',
            element: document.querySelector('.jpom-notify'),
            intro: '如果触发脚本无法选择，请先进入脚本管理菜单中维护脚本'
          }]
        }).start();
        return false;
      }
      this.$introJs().exit();
    },
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
      })
    },
		// 加载数据
    loadData() {
      this.loading = true;
      getPackageList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },

    // 新增
    handleAdd() {
      this.editPackageVisible = true;

      // 页面初始化
			this.uploadFileList = [];
			// this.temp.packageName = '';
			// this.temp.packageVersion = '';
			// this.temp.scriptId = '';
			// 新增or 编辑的标识
			this.temp = {
				type: 'add'
			};

			// 导航
      this.$nextTick(() => {
        setTimeout(() => {
          // this.introGuide();
        }, 500);
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.editPackageVisible = true;
			this.uploadFileList = [];
			this.temp.type = 'edit';
    },
    handleEditPkgOk() {
      // 检验表单
      this.$refs['editPkgForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 校验是否上传应用
				if (!this.uploadSuccess){
					this.$notification.warning({
						message: "请先点击[开始上传]上传应用.",
						duration: 2
					});
					return false
				}

				// 参数
        const params = {
          ...this.temp,
					packageFile: this.uploadFileList[0].name,
					packageName: this.temp.packageName ,
					packageVersion: this.temp.packageVersion,
					type: this.temp.type
        }
				editPkg(params).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editPkgForm'].resetFields();
            this.editPackageVisible = false;
            this.uploadSuccess = false;
            this.loadData();
          }
        })
      })
    },
		// 下载
		handleDownLoad(record) {
			// 请求参数
			const params = {
				id: record.id
			}
			// 请求接口拿到 blob
			downloadFile(params).then(blob => {
				const url = window.URL.createObjectURL(blob);
				let link = document.createElement('a');
				link.style.display = 'none';
				link.href = url;
				link.setAttribute('download', record.packageFile);
				document.body.appendChild(link);
				link.click();
			})
		},
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除应用么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
					deletePackage(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.loadData();
            }
          })
        }
      });
    },
		handleRemove(file) {
			const index = this.uploadFileList.indexOf(file);
			const newFileList = this.uploadFileList.slice();
			newFileList.splice(index, 1);
			this.uploadFileList = newFileList;
			this.uploadSuccess = false ;
		},
		beforeUpload(file) {
			//this.uploadFileList = [...this.uploadFileList, file];
			// 只允许上传单个文件
			this.uploadFileList = [file];
			return false;
		},

		// 开始上传文件
		startUpload() {
			const formData = new FormData();
			formData.append('file', this.uploadFileList[0]);
			// 上传文件
			uploadFile(formData).then(res => {
				if (res.code === 200) {
					this.$notification.success({
						message: res.msg,
						duration: 2,
					});
					this.uploadSuccess = true
				}
			})
			// 不置为空, 展示文件名
			//this.uploadFileList = [];
		}
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
.ant-btn {
  margin-right: 10px;
}
</style>
