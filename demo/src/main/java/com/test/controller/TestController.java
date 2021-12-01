package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.test.dao.TestMapper;
import com.test.service.TestService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TestController {
	/**
	 * 注入主数据源
	 */
	@Autowired
	private TestMapper testMapper;
	
	/**
	 * 注入动态数据源
	 */
	@Autowired
	private TestService testService;
	
	@RequestMapping("/name")
	public String name(@RequestParam(name = "db", defaultValue = "primary") String dbname) {
		if (dbname.equals("primary") || dbname.equals("second") || dbname.equals("third")) {
			if (dbname.equals("primary")) { // 使用主数据源
				return testMapper.name();
			} else { // 动态获取需要的数据源
				try {
					return testService.name(dbname);
				} catch (Exception e) {
					log.error("", e);
					return "DBError";
				}
			}
		} else {
			return "Invalid dbname.";
		}
	}
}
