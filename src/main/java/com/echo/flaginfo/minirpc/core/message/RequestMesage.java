package com.echo.flaginfo.minirpc.core.message;

import java.util.Arrays;
import java.util.UUID;

public class RequestMesage implements Message {
	
	private static final long serialVersionUID = 6907285084240098238L;

	private String id = UUID.randomUUID().toString();

	// 类名
	private String serviceName;

	// 方法名
	private String methodName;

	// 参数
	private Object[] args;

	// 参数类型
	private Class[] argsClassTypes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Class[] getArgsClassTypes() {
		return argsClassTypes;
	}

	public void setArgsClassTypes(Class[] argsClassTypes) {
		this.argsClassTypes = argsClassTypes;
	}

	
	@Override
	public String getMessageId() {
		return this.getId();
	}

	@Override
	public String toString() {
		return "RequestMesage [id=" + id + ", serviceName=" + serviceName + ", methodName=" + methodName + ", args="
				+ Arrays.toString(args) + ", argsClassTypes=" + Arrays.toString(argsClassTypes) + "]";
	}
}
