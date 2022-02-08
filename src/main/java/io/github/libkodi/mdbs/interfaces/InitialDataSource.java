package io.github.libkodi.mdbs.interfaces;

import io.github.libkodi.mdbs.MultiDataSource;
import io.github.libkodi.mdbs.entity.Connection;

public interface InitialDataSource {
	public void init(MultiDataSource context, String id, Connection conn);
}
