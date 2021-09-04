import axios from './config';




/**************************Begin Here***********************************/

/**
 * 上传文件到 （当前项目路径的地址/install-packages ）
 * @param {
 *  file: 文件 multipart/form-data,
 * }
 * formData
 */
export function uploadFile(formData) {
	return axios({
		url: '/onekey/pkg/upload',
		headers: {
			'Content-Type': 'multipart/form-data;charset=UTF-8'
		},
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
		data: formData
	})
}

// package 列表
export function getPackageList() {
	return axios({
		url: '/onekey/pkg/list_data.json',
		method: 'post'
	})
}

/**
 * 编辑  PKG
 * @param {*} params
 * params.type = {'add': 表示新增, 'edit': 表示修改}
 */
export function editPkg(params) {
	const data = {
		type: params.type,
		id: params.id,
		packageName: params.packageName,
		packageFile: params.packageFile,
		packageVersion: params.packageVersion
	}
	return axios({
		url: '/onekey/pkg/save.json',
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
		data
	})
}


/**
 * 删除文件
 * @param {id, path, name} params x
 */
export function deletePackage(id) {
	return axios({
		url: '/onekey/pkg/delete.json',
		method: 'post',
		data: {id}
	})
}

/**
 * 下载文件
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {id, path, name} params
 */
export function downloadFile(params) {
	return axios({
		url: '/onekey/pkg/download.html',
		method: 'get',
		responseType: 'blob',
		timeout: 0,
		params
	})
}
