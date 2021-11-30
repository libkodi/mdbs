package io.github.libkodi.mdbs.interfaces;

import org.apache.ibatis.session.SqlSession;

public interface SqlSessionCallback<T> {
	public T call(SqlSession session);
}
