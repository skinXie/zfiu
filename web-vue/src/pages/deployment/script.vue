<template>
  <div>
		<div ref="filter" class="filter">
			<a-button type="primary" @click="handleAdd">新增</a-button>
			<a-button type="primary" @click="handleUpload">上传脚本</a-button>
			<a-button type="primary" @click="loadData">刷新</a-button>
		</div>

    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :pagination="pagination" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 790, y: tableHeight - 120}" bordered
      :rowKey="(record, index) => index" @change="change">

			<a-tooltip slot="scriptId" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="scriptName" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="modifyTime" slot-scope="text" placement="topLeft" :title="text">
				<span>{{ text }}</span>
			</a-tooltip>

			<a-tooltip slot="packageModel.packageName" slot-scope="text" placement="topLeft" :title="text">
			<span>{{ text }}</span>
			</a-tooltip>

			<template slot="operation" slot-scope="text, record">
				<a-button type="primary" @click="handleBind(record)">关联应用</a-button>
				<a-button type="primary" @click="handleEdit(record)">编辑</a-button>
				<a-button type="danger" @click="handleDelete(record)">删除</a-button>
			</template>

    </a-table>

		<!-- 编辑区 -->
		<a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="700px">
			<a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
				<a-form-model-item label="Script 名称" prop="scriptName">
					<a-input v-model="temp.scriptName" placeholder="名称"/>
				</a-form-model-item>
				<a-form-model-item label="Script 内容" prop="scriptContext">
					<a-input v-model="temp.scriptContext" type="textarea" :rows="10" style="resize: none" placeholder="Script 内容(请不要编写阻塞脚本!!!)"/>
				</a-form-model-item>
			</a-form-model>
		</a-modal>

		<!-- 绑定区域 -->
		<a-modal v-model="editBindVisible" title="关联应用" @ok="handleBindOk" :maskClosable="false" width="700px">

			<a-form-model ref="editBindForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
				<a-form-model-item label="应用" prop="packageModel.packageName">
					<a-select v-model="zfpkg"
							placeholder="点击选择应用"
							@change="handleSelectChange">
						<a-select-option  v-for="pkg in packageList" :key="pkg.id">{{ pkg.packageName }}</a-select-option>
					</a-select>
				</a-form-model-item>

			</a-form-model>

		</a-modal>


		<!-- 上传文件 -->
		<a-modal v-model="uploadFileVisible" width="300px" title="上传 bat|bash 文件" :footer="null" :maskClosable="true">
			<a-upload-dragger :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="'.bat,.sh'">
				<a-button><a-icon type="upload" />选择 bat|bash 文件</a-button>
			</a-upload-dragger>
			<br/>
			<a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
		</a-modal>

  </div>
</template>
<script>

import { parseTime } from '../../utils/time';
import {editScript,getScriptList,uploadScriptFile,deleteScript,bindScript} from "@/api/install-script";
import {getPackageList} from "@/api/install-package";
export default {
  data() {
    return {
      loading: false,
			// 应用列表
			packageList:[],
			// 脚本列表
      list: [],
			// 绑定的下拉列表
			zfpkg:[],

      tableHeight: '70vh',
      temp: {},
			// 编辑视图
			editScriptVisible: false,
			// 绑定视图
			editBindVisible: false,
			uploadFileList: [],
			uploadFileVisible: false,

			columns: [
        {title: 'Script ID', dataIndex: 'id', width: 150, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: 'Script 名称', dataIndex: 'scriptName', width: 100,  ellipsis: true, scopedSlots: {customRender: 'scriptName'}},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          return parseTime(text);
        }, width: 100},
				{title: '关联应用', dataIndex: 'packageModel.packageName', width: 100,  ellipsis: true, scopedSlots: {customRender: 'packageModel.packageName'}},

				{title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 150}
      ],
			rules: {
				scriptName: [
					{ required: true, message: 'Please input Script name', trigger: 'blur' }
				],
				scriptContext: [
					{ required: true, message: 'Please input Script context', trigger: 'blur' }
				]
			}
    }
  },
  computed: {
  },
  created() {
    this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
      })
    },

    // 加载数据
    loadData() {
      this.loading = true;
			getScriptList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },

		// 添加
		handleAdd() {
			this.temp = {
				type: 'add'
			};
			this.editScriptVisible = true;
		},
		// 修改
		handleEdit(record) {
			this.temp = {
				type: 'edit'
			};
			this.temp = Object.assign(record);
			this.editScriptVisible = true;
		},
		// 修改
		handleBind(record) {
			this.temp = Object.assign(record);
			this.editBindVisible = true;
			// 加载应用列表
			this.loadPackageList();
		},
		// 加载 package
		loadPackageList() {
			getPackageList().then(res => {
				if (res.code === 200) {
					this.packageList = res.data;
				}
			})
		},
		// 绑定页面-选中应用下拉列表触发的动作
		handleSelectChange(value){
			this.temp = {
				scriptId:this.temp.id,
				packageId: value
			};
		},
		// 绑定页面-确定触发
		handleBindOk(){
			// 组装参数
			const params = {
				...this.temp
			}
			bindScript(params).then(res => {
				if (res.code === 200) {
					// 成功
					this.$notification.success({
						message: res.msg,
						duration: 2
					});
					// 关闭显示框
					this.editBindVisible = false;
					// 重置下拉框
					this.zfpkg = []
					// 重新加载数据
					this.loadData();
				}
			})
		},
		// 提交 Script 数据
		handleEditScriptOk() {
			// 检验表单
			this.$refs['editScriptForm'].validate((valid) => {
				if (!valid) {
					return false;
				}

				// 参数
				const params = {
					...this.temp,
					scriptName: this.temp.scriptName,
					scriptContext: this.temp.scriptContext
				}

				// 提交数据
				editScript(params).then(res => {
					if (res.code === 200) {
						// 成功
						this.$notification.success({
							message: res.msg,
							duration: 2
						});
						this.$refs['editScriptForm'].resetFields();
						this.editScriptVisible = false;
						this.loadData();
					}
				})
			})
		},
		// 准备上传文件
		handleUpload(record) {
			this.temp = Object.assign(record);
			this.uploadFileVisible = true;
		},
		handleRemove(file) {
			const index = this.uploadFileList.indexOf(file);
			const newFileList = this.uploadFileList.slice();
			newFileList.splice(index, 1);
			this.uploadFileList = newFileList;
		},
		beforeUpload(file) {
			// this.uploadFileList = [...this.uploadFileList, file];
			// 只允许上传单个文件
			this.uploadFileList = [file];
			return false;
		},
		// 开始上传文件
		startUpload() {
			this.uploadFileList.forEach(file => {
				const formData = new FormData();
				formData.append('file', file);
				// 上传文件
				uploadScriptFile(formData).then(res => {
					if (res.code === 200) {
						this.$notification.success({
							message: res.msg,
							duration: 2
						});
						this.uploadFileVisible = false;
					}
				})
			})
			setTimeout(() => {
				this.loadData();
			}, 2000)
			this.uploadFileList = [];
		},
		// 删除
		handleDelete(record) {
			this.$confirm({
				title: '系统提示',
				content: '真的要删除该脚本么？',
				okText: '确认',
				cancelText: '取消',
				onOk: () => {
					// 组装参数
					const params = {
						id: record.id
					}
					// 删除
					deleteScript(params).then((res) => {
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
.filter-item {
  width: 150px;
  margin-right: 10px;
}
</style>
