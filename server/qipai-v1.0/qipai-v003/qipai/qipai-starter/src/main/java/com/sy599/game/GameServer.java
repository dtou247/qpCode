package com.sy599.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import com.sy.general.BeanHelper;
import com.sy.general.PackageHelper;
import com.sy.mainland.util.MD5Util;
import com.sy.mainland.util.PropertiesFileLoader;
import com.sy.mainland.util.redis.Redis;
import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.constant.SharedConstants;
import com.sy599.game.common.constant.SystemCommonInfoType;
import com.sy599.game.common.datasource.DataSourceManager;
import com.sy599.game.common.executor.TaskExecutor;
import com.sy599.game.common.executor.task.*;
import com.sy599.game.db.bean.SystemCommonInfo;
import com.sy599.game.db.dao.SystemCommonInfoDao;
import com.sy599.game.gcommand.BaseCommand;
import com.sy599.game.manager.DBManager;
import com.sy599.game.manager.PlayerManager;
import com.sy599.game.manager.ServerManager;
import com.sy599.game.manager.SqlUpdateManage;
import com.sy599.game.manager.SystemCommonInfoManager;
import com.sy599.game.qipai.AbstractBaseCommandProcessor;
import com.sy599.game.robot.RobotManager;
import com.sy599.game.staticdata.StaticDataManager;
import com.sy599.game.udplog.UdpLogger;
import com.sy599.game.util.*;
import com.sy599.game.util.helper.ResourceWatchHandler;
import com.sy599.game.util.mj.util.MjHuUtil;
import com.sy599.game.websocket.netty.WebSocketServer;
import net.sy599.common.security.SecuritConstant;
import net.sy599.common.security.SecuritConstantImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;

public class GameServer {
	private static final String PROPERTIES_CONFIG_DIR = "WEB-INF/config/";

	private static String realPath = "";//"WebRoot/";

	public static void main(String[] args) throws Exception {
		try {
			long start = TimeUtil.currentTimeMillis();
			PropertiesFileLoader.loadClassPath(GameServer.class);
			realPath = PropertiesFileLoader.getWebRoot();

			// ???????????????
			if (!init()) {
				LogUtil.errorLog.error("init fail----????????????----");
				System.exit(0);
				return;
			}
//			 PushGroupRedBagRainConfigTask.test();
			// ??????????????????
			startSchTask();
			LogUtil.monitorLog.info("GameServer|startSchTask|" + (System.currentTimeMillis() - start));

			UdpLogger.getInstance().connectToServer();
			LogUtil.monitorLog.info("GameServer|UdpLogger.connectToServer|" + (System.currentTimeMillis() - start));

			// ??????????????????
			memoryInfo();
			// ???????????????
			SystemCommonInfoManager.getInstance().updateGameVersion(SharedConstants.version);
			SystemCommonInfoManager.getInstance().updateGameStartUpTime(TimeUtil.formatTime(TimeUtil.now()));
			SystemCommonInfoManager.getInstance().saveDB(false);
			LogUtil.monitorLog.info("GameServer|checkGameServerMark|" + (System.currentTimeMillis() - start));

			WebSocketServer server;
			try {
				server = new WebSocketServer(GameServerConfig.SERVER_PORT);
			} catch (Exception e) {
				LogUtil.e("start err:" + e.getMessage(), e);
				server = null;
			}

			if (server != null && server.start()) {
				PropertiesFileLoader loader = new PropertiesFileLoader();
				loader.init();

				Runtime.getRuntime().addShutdownHook(ShutdownManager.getInstance(server, loader));
				Thread.setDefaultUncaughtExceptionHandler(ShutdownManager.getInstance(server, loader));

				LogUtil.monitorLog.info("GameServer|started|" + (System.currentTimeMillis() - start));

				if (PropertiesFileLoader.isWindows()) {
					Scanner scan;
					String read;
					do {
						System.out.println("??????exit(???q)??????");
						scan = new Scanner(System.in);
						read = scan.nextLine();
						System.out.println("???????????????" + read);
					} while (!"exit".equals(read) && !"q".equals(read));
					System.exit(0);
				}
			} else {
				LogUtil.errorLog.error("GameServer|start|error, closing");
				ShutdownManager.getInstance(server).shutdownNow();
				LogUtil.errorLog.error("GameServer|start|error|closedFinished");
				System.exit(0);
			}
		} catch (Exception e) {
			LogUtil.errorLog.error("GameServer|start|error|" + e.getMessage(), e);
			System.exit(0);
		}
	}

	/**
	 * ?????????????????????
	 */
	private static void memoryInfo() {
		int freeMem = MemoryUtil.getFreeMemoryMB();
		int totalMem = MemoryUtil.getMaxMemoryMB();
		LogUtil.monitorLog.info((new StringBuilder("free memory ")).append(freeMem).append(" Mb of ").append(totalMem).append(" Mb").toString());
	}

	private static void startSchTask() {
		//???????????????????????????
		int saveDBinterval = 60 * SharedConstants.SENCOND_IN_MINILLS;
		if (GameServerConfig.SERVER_PF.equals("debug")) {
			saveDBinterval = 2 * 1000;
		}
		TaskExecutor.getInstance().scheduleWithFixedDelay(new HeartBeatTask(), saveDBinterval, saveDBinterval);
		TaskExecutor.getInstance().submitSchTask(new ServerConfigTask(), 30 * SharedConstants.SENCOND_IN_MINILLS, 60 * SharedConstants.SENCOND_IN_MINILLS);
		TaskExecutor.getInstance().submitSchTask(new TenSencondsTask(), 5 * SharedConstants.SENCOND_IN_MINILLS, 10 * SharedConstants.SENCOND_IN_MINILLS);
		TaskExecutor.getInstance().scheduleWithFixedDelay(new CompetitionTask(), SharedConstants.SENCOND_IN_MINILLS, SharedConstants.SENCOND_IN_MINILLS, true);
		TaskExecutor.getInstance().scheduleWithFixedDelay(new GoldRoomMatchTask(), SharedConstants.SENCOND_IN_MINILLS, 3 * SharedConstants.SENCOND_IN_MINILLS, true);
        TaskExecutor.getInstance().scheduleWithFixedRate(new TenMinuteFixedRateTask(), TenMinuteFixedRateTask.loadFirstExecuteDate(), 10 * 60 * 1000);
        TaskExecutor.getInstance().submitSchTask(new DayTask(), 0, 24 * 60 * 60 * SharedConstants.SENCOND_IN_MINILLS);
        TaskExecutor.getInstance().scheduleWithFixedRate(new OneMinuteFixedRateTask(), OneMinuteFixedRateTask.loadFirstExecuteDate(), 60 * SharedConstants.SENCOND_IN_MINILLS);
        TaskExecutor.getInstance().submitSchTask(new ZeroUpdateTask(), ZeroUpdateTask.getZeroDelay(), 24 * 60 * 60 * SharedConstants.SENCOND_IN_MINILLS);
		TaskExecutor.getInstance().submitSchTask(new PushGroupRedBagRainConfigTask(), PushGroupRedBagRainConfigTask.getNextMinDelay(),  60 * SharedConstants.SENCOND_IN_MINILLS);

	}



	private static void initTableAndPlayerMsgs() throws Exception {

		//??????player ??? CommandProcessor
		Set<Class<?>> classSet = PackageHelper.getClasses("com.sy599.game.qipai", true, Player.class);
		for (Class<?> cls : classSet) {
			BeanHelper.getMethod(cls, "loadWanfaPlayers", cls.getClass()).invoke(null, cls);
			LogUtil.monitorLog.info("load Player Game yes:" + cls.getName());
		}

		//??????table
		classSet = PackageHelper.getClasses("com.sy599.game.qipai", true, BaseTable.class);
		for (Class<?> cls : classSet) {
			BeanHelper.getMethod(cls, "loadWanfaTables", cls.getClass()).invoke(null, cls);
			LogUtil.monitorLog.info("load Table Game yes:" + cls.getName());
		}

		//????????????
		classSet = PackageHelper.getClasses("com.sy599.game.qipai", true, AbstractBaseCommandProcessor.class);
		Set<AbstractBaseCommandProcessor> commandSet = PlayerManager.playerTypes.keySet();
		for (Class<?> cls : classSet) {
			boolean canLoad = false;
			for (AbstractBaseCommandProcessor command : commandSet) {
				if (cls == command.getClass()) {
					canLoad = true;
					break;
				}
			}
			if (!canLoad && !GameServerConfig.isDebug()) {
				LogUtil.monitorLog.warn("not load command processor:" + cls.getName());
				continue;
			} else {
				LogUtil.monitorLog.info("yes load command processor:" + cls.getName());
			}

			Set<Field> set = BeanHelper.getAllFields(cls);
			for (Field field : set) {
				String name = field.getName();
				Object value;
				try {
					field.setAccessible(true);
					value = field.get(name);
				} catch (Exception e) {
					value = null;
				}

				if (value != null) {
					if (value instanceof Map) {
						Map map = (Map) value;
						Iterator it = map.entrySet().iterator();
						while (it.hasNext()) {
							Object temp1 = it.next();
							if (temp1 instanceof Map.Entry) {
								Map.Entry kv = (Map.Entry) temp1;
								if ((kv.getKey() instanceof Number) && (kv.getValue() instanceof Class) && (BaseCommand.class.isAssignableFrom((Class<?>) kv.getValue()))) {
									Commands.registCommand((Class<? extends AbstractBaseCommandProcessor>) cls, ((Number) kv.getKey()).shortValue());
									LogUtil.monitorLog.info("load command msgType={},target={}", kv.getKey(), kv.getValue());
								}
							}
						}
					}
				}
			}
		}
	}


	private static boolean init() throws Exception {

		// ??????
		String path = realPath + GameServerConfig.log4j_config_dir;
		PropertyConfigurator.configure(path);
		LogUtil.monitorLog.info("GameServer|start");
		LogUtil.monitorLog.info("GameServer|start|initLogFinished|" + path);
		long start = System.currentTimeMillis();


		//??????????????????????????????GameServerConfig???
		loadServerConfig();
		LogUtil.monitorLog.info("GameServer|loadServerConfig|" + (System.currentTimeMillis() - start));

		LogUtil.monitorLog.info("redis status:connected=" + Redis.isConnected());

		// ????????????????????????
		initDataBase();
		LogUtil.monitorLog.info("GameServer|initDataBase|" + (System.currentTimeMillis() - start));

		GroupRedBagRainUtil.initRedBagRainConfigs();
		LogUtil.monitorLog.info("GameServer|GroupRedBagRainUtil.initRedBagRainConfigs " + (System.currentTimeMillis() - start));

		//???????????????
		int count = ServerManager.init();
		LogUtil.monitorLog.info("GameServer|ServerManager.init:count=" + count + "|" + (System.currentTimeMillis() - start));

		// ?????????????????????
		path = realPath + "WEB-INF/" + GameServerConfig.csv_path + "/";
		StaticDataManager.init(path);
		LogUtil.monitorLog.info("GameServer|StaticDataManager.init:path=" + path + "|" + (System.currentTimeMillis() - start));

		// ??????????????????
		path = realPath + GameServerConfig.keyWord_dir;
		KeyWordsFilter.getInstance().initData(path);
		LogUtil.monitorLog.info("GameServer|KeyWordsFilter.initData:path=" + path + "|" + (System.currentTimeMillis() - start));

		// ????????????????????????
		path = realPath + "WEB-INF/csv/robotNames.txt";
		DataLoaderUtil.initRobotNames(path);
		LogUtil.monitorLog.info("GameServer|DataLoaderUtil.initRobotNames:path=" + path + "|" + (System.currentTimeMillis() - start));

		LogUtil.monitorLog.info("---------init csv path::" + GameServerConfig.csv_path + "-----------");



		/**
		 * ??????????????????server???????????????
		 */
		ResourcesConfigsUtil.initResourcesConfigs();
		LogUtil.monitorLog.info("GameServer|initResourcesConfigs|" + (System.currentTimeMillis() - start));

		GroupConfigUtil.initGroupConfig();
		LogUtil.monitorLog.info("GameServer|initGroupConfig|" + (System.currentTimeMillis() - start));

		//?????????????????????????????????
		loadConfigs();
		LogUtil.monitorLog.info("GameServer|loadConfigs|" + (System.currentTimeMillis() - start));

		//???????????????????????????(table/player)
		initTableAndPlayerMsgs();
		LogUtil.monitorLog.info("GameServer|initTableAndPlayerMsgs|" + (System.currentTimeMillis() - start));

		/*????????????????????????*/
		SharedConstants.loadTimeOut();
		LogUtil.monitorLog.info("GameServer|SharedConstants.loadTimeOut|" + (System.currentTimeMillis() - start));

		// ????????????????????????
		GameServerConfig.loadSystemCommonInfo();
		LogUtil.monitorLog.info("GameServer|loadSystemCommonInfo|" + (System.currentTimeMillis() - start));

		// ????????????????????????
		TingResouce.init();
		LogUtil.monitorLog.info("GameServer|TingResouce.init|" + (System.currentTimeMillis() - start));

		// //////////////
		// ?????????????????????
		SystemCommonInfo info = SystemCommonInfoDao.getInstance().select(SystemCommonInfoType.isStartGame.name());
		if (info != null) {
			String flag = info.getContent();
			String mark = GeneralUtil.loadCurrentServerMsg();
			String md5 = MD5Util.getMD5String(mark);
			LogUtil.monitorLog.info("GameServer|CurrentServerMsg-->flag:{},md5:{},msg:{}", flag, md5, mark);
			if (StringUtils.isNotBlank(flag) && (!"0".equals(flag)) && ("1".equals(flag) || !md5.equalsIgnoreCase(flag))) {
				LogUtil.e("??????????????????????????????-->" + GameServerConfig.SERVER_ID);
				LogUtil.msg("??????????????????????????????-->" + GameServerConfig.SERVER_ID);
				return false;
			}

			if (SystemCommonInfoManager.getInstance().updateStartGame(info, SharedConstants.game_flag_start) == 1) {
				LogUtil.msg("???????????????????????????-->" + GameServerConfig.SERVER_ID);
			} else {
				LogUtil.msg("???????????????????????????-->" + GameServerConfig.SERVER_ID);
			}
		}

		LogUtil.monitorLog.info("GameServer|checkGameServerStart|" + (System.currentTimeMillis() - start));

		// //////////////

		SqlUpdateManage.getInstance().check();
		LogUtil.monitorLog.info("GameServer|checkSqlUpdate|" + (System.currentTimeMillis() - start));

		// ????????????????????????
		DBManager.loadDataFromDB();
		LogUtil.monitorLog.info("GameServer|loadDataFromDB|" + (System.currentTimeMillis() - start));

		//???????????????
		ResourceWatchHandler.getWatchInstance(realPath + PROPERTIES_CONFIG_DIR, realPath + "WEB-INF/csv/").initResourceHandler();

		LogUtil.monitorLog.info("GameServer|getWatchInstance|" + (System.currentTimeMillis() - start));

		// ????????????????????????
		MjHuUtil.init();
		LogUtil.monitorLog.info("GameServer|MjHuData.init|" + (System.currentTimeMillis() - start));

		SysPartitionUtil.init();
		LogUtil.monitorLog.info("GameServer|SysPartitionUtil.init|" + (System.currentTimeMillis() - start));

		GoldRoomUtil.init();
		LogUtil.monitorLog.info("GameServer|GoldRoomUtil.initConfig|" + (System.currentTimeMillis() - start));

		SoloRoomUtil.init();
		LogUtil.monitorLog.info("GameServer|SoloRoomUtil.initConfig|" + (System.currentTimeMillis() - start));

        CompetitionUtil.init();
        LogUtil.monitorLog.info("GameServer|CompetitionUtil.initConfig|" + (System.currentTimeMillis() - start));

        GoldRoomMatchUtil.initFromDBOnServerStart();
        LogUtil.monitorLog.info("GameServer|GoldRoomMatchUtil.initFromDBOnServerStart|" + (System.currentTimeMillis() - start));

        RobotManager.init();
        LogUtil.monitorLog.info("GameServer|RobotManager.init|" + (System.currentTimeMillis() - start));

        return true;
    }

	/**
	 * ??????????????????????????????GameServerConfig???
	 *
	 * @throws Exception
	 */
	private static void loadServerConfig() throws Exception {
		Properties serverProperties = new Properties();
		loadFromFile(serverProperties, realPath + GameServerConfig.server_config_dir);
		GameServerConfig.load(serverProperties);

	}

	/**
	 * ???????????????????????????
	 *
	 * @throws Exception
	 */
	private static void loadConfigs() throws Exception {
		Properties payProperties = new Properties();
		loadFromFile(payProperties, realPath + PROPERTIES_CONFIG_DIR + "pay.properties");
		PayConfigUtil.load(payProperties);

		Properties gameProperties = new Properties();
		loadFromFile(gameProperties, realPath + PROPERTIES_CONFIG_DIR + "gameConfig.properties");
		GameConfigUtil.load(gameProperties);
	}

	private static void initDataBase() throws Exception {
		Reader reader;
		Properties jdbcProperties = new Properties();
		loadFromFile(jdbcProperties, realPath + GameServerConfig.jdbc_config_dir);
		reader = Resources.getResourceAsReader(GameServerConfig.ibatis_config_dir);
		String md5Password = jdbcProperties.getProperty("jdbc.password");
		SecuritConstant des = new SecuritConstantImpl();
		String url = jdbcProperties.getProperty("jdbc.url");
		jdbcProperties.setProperty("jdbc.password", des.decrypt(md5Password));
		DataSourceManager.setServerSqlMapClient(SqlMapClientBuilder.buildSqlMapClient(reader, jdbcProperties), loadDbNameFromUrl(url));
		reader.close();

		reader = Resources.getResourceAsReader(GameServerConfig.ibatis_config_dir);
		String loginpw = jdbcProperties.getProperty("jdbc.loginpassword");
		url = jdbcProperties.getProperty("jdbc.loginurl");
		Properties jdbc = new Properties();
		jdbc.setProperty("jdbc.password", des.decrypt(loginpw));
		jdbc.setProperty("jdbc.url", url);
		jdbc.setProperty("jdbc.user", jdbcProperties.getProperty("jdbc.loginuser"));
		jdbc.setProperty("jdbc.ClassDriver", jdbcProperties.getProperty("jdbc.ClassDriver"));
		DataSourceManager.setLoginSqlMapClient(SqlMapClientBuilder.buildSqlMapClient(reader, jdbc), loadDbNameFromUrl(url));
		reader.close();

		String loginSlaveUser = jdbcProperties.getProperty("jdbc.login.slave.user");
		if (loginSlaveUser != null && loginSlaveUser.length() > 0) {
			String loginSlavePw = jdbcProperties.getProperty("jdbc.login.slave.password");
			String loginSlaveUrl = jdbcProperties.getProperty("jdbc.login.slave.url");
			Reader loginSlaveReader = Resources.getResourceAsReader(GameServerConfig.ibatis_config_dir);
			Properties loginSalveJdbc = new Properties();
			loginSalveJdbc.setProperty("jdbc.password", des.decrypt(loginSlavePw));
			loginSalveJdbc.setProperty("jdbc.url", loginSlaveUrl);
			loginSalveJdbc.setProperty("jdbc.user", loginSlaveUser);
			loginSalveJdbc.setProperty("jdbc.ClassDriver", jdbcProperties.getProperty("jdbc.ClassDriver"));
			DataSourceManager.setLoginSlaveSqlMapClient(SqlMapClientBuilder.buildSqlMapClient(loginSlaveReader, loginSalveJdbc), loadDbNameFromUrl(loginSlaveUrl));
			loginSlaveReader.close();
		}

		reader = Resources.getResourceAsReader(GameServerConfig.ibatis_config_dir);
		loginpw = jdbcProperties.getProperty("jdbc.funcpassword");
		if (StringUtils.isBlank(loginpw)) {
			reader.close();
			return;
		}
		url = jdbcProperties.getProperty("jdbc.funcurl");
		jdbc = new Properties();
		jdbc.setProperty("jdbc.password", des.decrypt(loginpw));
		jdbc.setProperty("jdbc.url", url);
		jdbc.setProperty("jdbc.user", jdbcProperties.getProperty("jdbc.funcuser"));
		jdbc.setProperty("jdbc.ClassDriver", jdbcProperties.getProperty("jdbc.ClassDriver"));
		DataSourceManager.setLogSqlMapClient(SqlMapClientBuilder.buildSqlMapClient(reader, jdbc), loadDbNameFromUrl(url));
		reader.close();

	}

	private static String loadDbNameFromUrl(String jdbcUrl) {
		String dbName = null;
		if (jdbcUrl != null) {
			int idx = jdbcUrl.indexOf("?");
			if (idx >= 0) {
				String temp = jdbcUrl.substring(0, idx);
				idx = temp.lastIndexOf("/");
				if (idx >= 0) {
					dbName = temp.substring(idx + 1);
				}
			}
		}

		return dbName;
	}

	private static void loadFromFile(Properties properties, String dir) throws Exception {
		if (!new File(dir).exists()) {
			LogUtil.msgLog.warn("file not exists:" + dir);
			return;
		}
		try {
			FileInputStream fis = new FileInputStream(dir);
			properties.load(fis);
			fis.close();
		} catch (Exception e) {
			throw e;
		}
	}

}
