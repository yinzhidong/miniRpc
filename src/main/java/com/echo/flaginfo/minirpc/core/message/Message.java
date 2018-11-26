package com.echo.flaginfo.minirpc.core.message;

import java.io.Serializable;

public interface Message extends Serializable{

	/**
	 * 获取消息体id
	 * @return
	 */
	String getMessageId();
	
}
