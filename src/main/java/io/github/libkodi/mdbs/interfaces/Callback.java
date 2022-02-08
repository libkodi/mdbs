package io.github.libkodi.mdbs.interfaces;

import org.apache.ibatis.session.SqlSession;

/**
 * 打开SQL会话回调
 *
 * @param <T>
 */
public interface Callback<T> {
	public T call(SqlSession session);
}
