package com.echo.flaginfo.minirpc.core.message;

public class ResponseMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -177990190494845116L;

	private String id;

	private ResultEntity resultEntity;

	private Object responseObject;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ResultEntity getResultEntity() {
		return resultEntity;
	}

	public void setResultEntity(ResultEntity resultEntity) {
		this.resultEntity = resultEntity;
	}

	public Object getResponseObject() {
		return responseObject;
	}

	public void setResponseObject(Object responseObject) {
		this.responseObject = responseObject;
	}

	@Override
	public String getMessageId() {
		return this.getId();
	}

	@Override
	public String toString() {
		return "ResponseMessage [id=" + id + ", resultEntity=" + resultEntity + ", responseObject=" + responseObject
				+ "]";
	}
	
}
