package com.echo.flaginfo.minirpc.core.message;

public enum ResultEntity {

	SUCCESS(0, "成功"), FAIL(-1, "失败"),TIMEOUT(1001, "超时"), OTHER(9999, "系统错误");

	private int code;
	private String msg;
	
	private ResultEntity(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
