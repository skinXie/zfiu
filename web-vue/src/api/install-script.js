import axios from './config';

/**************************Begin Here***********************************/

/**
 * script 列表
 * @param {String} nodeId 节点 ID
 */
export function getScriptList() {
	return axios({
		url: '/onekey/script/list',
		method: 'post'
	})
}

/**
 * Script 编辑
 * params.name 名称
 * params.context 内容
 */
export function editScript(params) {
	return axios({
		url: '/onekey/script/save.json',
		method: 'post',
		data: params
	})
}

/**
 * 上传 Script 文件
 * @param {
 *  file: 文件 multipart/form-data
 * } formData
 */
export function uploadScriptFile(formData) {
	return axios({
		url: '/onekey/script/upload',
		headers: {
			'Content-Type': 'multipart/form-data;charset=UTF-8'
		},
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
		data: formData
	})
}


/**
 * 删除 Script
 * @param {id} params
 * params.id 编辑修改时判断 ID
 */
export function deleteScript(params) {
	return axios({
		url: '/onekey/script/del.json',
		method: 'post',
		data: params
	})
}

/**
 * 绑定 Script 和 package
 * @param {id} params
 * params.id 编辑修改时判断 ID
 */
export function bindScript(params) {
	return axios({
		url: '/onekey/script/bind.json',
		method: 'post',
		data: params
	})
}
