package com.sy.sanguo.game.pdkuai.manager;

import com.sy.sanguo.common.executor.HourTask;
import com.sy.sanguo.common.executor.MinuteTask;
import com.sy.sanguo.common.executor.PushGroupRedBagRainResultTask;
import com.sy.sanguo.common.util.TaskExecutor;
import com.sy.sanguo.game.dao.RoomDaoImpl;
import com.sy.sanguo.game.dao.SqlDao;
import com.sy.sanguo.game.dao.group.GroupDaoNew;
import com.sy.sanguo.game.pdkuai.db.dao.TableLogDao;
import com.sy.sanguo.game.pdkuai.util.LogUtil;
import com.sy599.sanguo.util.ResourcesConfigsUtil;
import com.sy599.sanguo.util.SysPartitionUtil;
import com.sy599.sanguo.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

public class TaskManager {
    private static TaskManager _inst = new TaskManager();

    private static final int delLimit = 10000;
    private static final int del_time_interval = 6000;

    public static TaskManager getInstance() {
        return _inst;
    }

    public void init() {
        TaskExecutor.getInstance().submitSchTask(new MinuteTask(), 0, TimeUtil.MIN_IN_MINILLS);
        TaskExecutor.getInstance().submitSchTask(new HourTask(), 60 * 60 * 1000, 6 * 60 * 60 * 1000);
        TaskExecutor.getInstance().submitSchTask(new PushGroupRedBagRainResultTask(), PushGroupRedBagRainResultTask.getNextMinDelay(),1000*60);
        // --------------------------------------------------
        // ------------每天凌晨04:30点执行任务-----------------
        // --------------------------------------------------
        Calendar c5 = Calendar.getInstance();
        if (c5.get(Calendar.HOUR_OF_DAY) > 4) {
            c5.add(Calendar.DAY_OF_YEAR, 1);
        } else if (c5.get(Calendar.HOUR_OF_DAY) == 4 && c5.get(Calendar.MINUTE) >= 30) {
            c5.add(Calendar.DAY_OF_YEAR, 1);
        }
        c5.set(Calendar.HOUR_OF_DAY, 4);
        c5.set(Calendar.MINUTE, 30);
        c5.set(Calendar.SECOND, 0);

        Timer timer5 = new Timer();
        timer5.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                recoverRoom();

                clearGoldRoom();

                clearTableRecord();

                clearTableUser();

                clearGroupTable();

                clearDataStatistics();

                clearUserPlayLog();

                clearGroupCreditLog();

                clearUserCardRecord();

                clearLogGroupTable();

                clearLogGroupCommission();

                clearBjdDataStatistics();

                clearOnlineData();

                clearLoginData();

                clearLogGroupUserAlert();

                clearUserStatistics();

                clearFirstmyth();

                clearSystemUserCountLogin();

                clearUserMessage();

                clearUserShare();

                clearUserDataStatistics();

                clearItemExchange();

                clearGroupReview();

                clearStatisticsPlatform();

                clearLogActivityReward();

                clearRoomcardConsumeStatistics();

                clearRoomgoldConsumeStatistics();

                clearRedbagRainResult();

            }
        }, c5.getTime(), 24 * 60 * 60 * 1000);


        // --------------------------------------------------
        // -----------------每天凌晨00:00执行任务--------------
        // --------------------------------------------------
        Calendar c0 = Calendar.getInstance();
        c0.set(Calendar.MINUTE, 0);
        c0.set(Calendar.SECOND, 1);
        if (c0.get(Calendar.HOUR_OF_DAY) >= 0) {
            c0.add(Calendar.DAY_OF_YEAR, 1);
        }
        c0.set(Calendar.HOUR_OF_DAY, 0);
        Timer timer0 = new Timer();
        timer0.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
//                refreshGroupLevelData();
            }
        }, c0.getTime(), 24 * 60 * 60 * 1000);


        // --------------------------------------------------
        // -----------------每天凌晨04:00执行任务--------------
        // --------------------------------------------------
        Calendar c4 = Calendar.getInstance();
        c4.set(Calendar.MINUTE, 0);
        c4.set(Calendar.SECOND, 1);
        if (c4.get(Calendar.HOUR_OF_DAY) >= 0) {
            c4.add(Calendar.DAY_OF_YEAR, 1);
        }
        c4.set(Calendar.HOUR_OF_DAY, 4);
        Timer timer4 = new Timer();
        timer4.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                processBjdNewerActivity();
            }
        }, c4.getTime(), 24 * 60 * 60 * 1000);

    }

    private void recoverRoom() {

        String currentDate = TimeUtil.formatXxDays_00(-7);
        try {
            long startTime = System.currentTimeMillis();
            int delCount = RoomDaoImpl.getInstance().recoverRoom(currentDate);
            LogUtil.i("clearData|TaskManager|recoverRoom|" + (System.currentTimeMillis() - startTime) + "|" + delCount + "|" + currentDate);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearTableRecord() {
        String currentDate = TimeUtil.formatXxDays_00(-6);

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";
        try {
            // ----------------------------- t_table_record ----------------------------------
            delName = "clearTableRecord";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_table_record where createdTime < '" + currentDate + "'";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from t_table_record where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }


    private void clearTableUser() {
        String currentDate = TimeUtil.formatXxDays_00(-6);

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";

        try {
            if (SysPartitionUtil.isWriteMaster()) {
                delName = "clearTableUser";
                maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_table_user where createdTime < '" + currentDate + "'";
                maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                if (maxKeyId > 0) {
                    delSql = "delete from t_table_user where keyId <= " + maxKeyId;
                    deleteDataForLogin(delName, delSql, delLimit);
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }

        try {
            // ----------------------------- t_table_user ----------------------------------
            if (SysPartitionUtil.isWritePartition()) {
                for (Integer seq : SysPartitionUtil.allGroupSeqList) {
                    delName = "clearTableUser_" + seq;
                    maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_table_user_" + seq + " where createdTime < '" + currentDate + "'";
                    maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                    if (maxKeyId > 0) {
                        delSql = "delete from t_table_user_" + seq + " where keyId <= " + maxKeyId;
                        deleteDataForLogin(delName, delSql, delLimit);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearGroupTable() {
        String currentDate = TimeUtil.formatXxDays_00(-6);

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";

        try {
            // ----------------------------- t_group_table ----------------------------------
            delName = "clearGroupTable1";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_group_table where overTime < '" + currentDate + "'";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from t_group_table where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }

        try {
            //清理未开局解散房间
            currentDate = TimeUtil.formatXxDays_00(0);
            delName = "clearGroupTable2";
            delSql = "delete from t_group_table where overTime <= '" + currentDate + "' and currentState = '3' ";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearGoldRoom() {
        String currentDate = TimeUtil.formatXxDays_00(-15);
        try {
            if (SqlDao.getInstance().checkExistsGoldRoomTable() > 0) {
                String delName = "clearGoldRoom1";
                String delSql = "delete from t_gold_room where createdTime <= '" + currentDate + "'";
                deleteDataForLogin(delName, delSql, delLimit);

                delName = "clearGoldRoom2";
                delSql = "delete from t_gold_room_user where createdTime <= '" + currentDate + "'";
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearDataStatistics() {
        SimpleDateFormat ym = new SimpleDateFormat("yyyyMM");
        SimpleDateFormat ymd = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat ymdh = new SimpleDateFormat("yyyyMMddHH");
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_YEAR, -40);
        String startDate1 = ym.format(cal.getTime());
        String startDate2 = ymd.format(cal.getTime());
        String startDate3 = ymdh.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -180);
        String endDate1 = ym.format(cal.getTime());
        String endDate2 = ymd.format(cal.getTime());
        String endDate3 = ymdh.format(cal.getTime());
        try {
            if (SqlDao.getInstance().checkExistsDataStatisticsTable() > 0) {

                String delSql;
                String delName = "clearDataStatistics1";
                delSql = "delete from t_data_statistics where dataDate <= " + startDate1 + " and dataDate >= " + endDate1;
                deleteDataForLogin(delName, delSql, delLimit);

                delName = "clearDataStatistics2";
                delSql = "delete from t_data_statistics where dataDate <= " + startDate2 + " and dataDate >= " + endDate2;
                deleteDataForLogin(delName, delSql, delLimit);

                delName = "clearDataStatistics3";
                delSql = "delete from t_data_statistics where dataDate <= " + startDate3 + " and dataDate >= " + endDate3;
                deleteDataForLogin(delName, delSql, delLimit);

            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 白金岛有效局数活动，新人
     */
    private void processBjdNewerActivity() {
        if ("1".equals(ResourcesConfigsUtil.loadServerPropertyValue("switch_bjdNewerActivity", "0"))) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -30);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String startDate1 = sdf.format(cal.getTime());
                long startTime = System.currentTimeMillis();
                if (SysPartitionUtil.isWriteMaster()) {
                    String sql1 = " DELETE FROM bjd_group_newer_bind " +
                            " WHERE " +
                            " userId IN ( " +
                            "   SELECT " +
                            "   d.userId " +
                            "   FROM " +
                            "       ( " +
                            "        SELECT " +
                            "        gu.userId " +
                            "        FROM " +
                            "        t_group_user gu " +
                            "        LEFT JOIN ( SELECT * FROM t_table_user WHERE createdTime > '" + startDate1 + "' ) tu ON tu.userId = gu.userId " +
                            "        WHERE " +
                            "           tu.keyId IS NULL " +
                            "       ) AS d " +
                            " ); ";
                    try {
                        int delCount = SqlDao.getInstance().delete(sql1);
                        LogUtil.i("clearData|TaskManager|2|processBjdNewerActivity1|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|sql=" + sql1);
                    } catch (Exception e) {
                        LogUtil.e("Exception:" + e.getMessage(), e);
                    }
                }
                if (SysPartitionUtil.isWritePartition()) {
                    for (Integer seq : SysPartitionUtil.allGroupSeqList) {
                        String sql1 = " DELETE FROM bjd_group_newer_bind " +
                                " WHERE " +
                                " userId IN ( " +
                                "   SELECT " +
                                "   d.userId " +
                                "   FROM " +
                                "       ( " +
                                "        SELECT " +
                                "        gu.userId " +
                                "        FROM " +
                                "        t_group_user gu " +
                                "        LEFT JOIN ( SELECT * FROM t_table_user_" + seq + " WHERE createdTime > '" + startDate1 + "' ) tu ON tu.userId = gu.userId " +
                                "        WHERE " +
                                "           tu.keyId IS NULL " +
                                "       ) AS d " +
                                " ); ";
                        try {
                            int delCount = SqlDao.getInstance().delete(sql1);
                            LogUtil.i("clearData|TaskManager|2|processBjdNewerActivity2|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|sql=" + sql1);
                        } catch (Exception e) {
                            LogUtil.e("Exception:" + e.getMessage(), e);
                        }
                    }
                }

            } catch (Exception e) {
                LogUtil.e("Exception:" + e.getMessage(), e);
            }
        }
    }

    /**
     * 清理信用分记录，15天
     */
    private void clearGroupCreditLog() {
        try {
            String clearDate = TimeUtil.formatXxDays_00(-6);
            if (SysPartitionUtil.isWriteMaster()) {
                String maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_group_credit_log where createdTime < '" + clearDate + "'";
                long maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                if (maxKeyId > 0) {
                    String delName = "clearGroupCreditLog";
                    String delSql = "delete from t_group_credit_log where keyId <= " + maxKeyId;
                    deleteDataForLogin(delName, delSql, delLimit);
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }

        try {
            String clearDate = TimeUtil.formatXxDays_00(-14);

            if (SysPartitionUtil.isWritePartition()) {
                for (Integer seq : SysPartitionUtil.allGroupSeqList) {
                    String maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_group_credit_log_" + seq + " where createdTime < '" + clearDate + "'";
                    long maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                    if (maxKeyId > 0) {
                        String delName = "clearGroupCreditLog_" + seq;
                        String delSql = "delete from t_group_credit_log_" + seq + " where keyId <= " + maxKeyId;
                        deleteDataForLogin(delName, delSql, delLimit);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
        try {
            String clearDate = TimeUtil.formatXxDays_00(-15);

            if (SysPartitionUtil.isWriteMaster()) {
                String maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_group_credit_log_transfer where createdTime < '" + clearDate + "'";
                long maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                if (maxKeyId > 0) {
                    String delName = "clearGroupCreditLogTransfer";
                    String delSql = "delete from t_group_credit_log_transfer where keyId <= " + maxKeyId;
                    deleteDataForLogin(delName, delSql, delLimit);
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }

        try {
            String clearDate = TimeUtil.formatXxDays_00(-15);

            if (SysPartitionUtil.isWriteMaster()) {
                String maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_group_credit_log_master where createdTime < '" + clearDate + "'";
                long maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
                if (maxKeyId > 0) {
                    String delName = "clearGroupCreditLogMaster";
                    String delSql = "delete from t_group_credit_log_master where keyId <= " + maxKeyId;
                    deleteDataForLogin(delName, delSql, delLimit);
                }
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 清理login库的数据
     *
     * @param delName  用于日志区分清理功能
     * @param sql      执行的sql
     * @param delLimit 单次最大清理数据量
     */
    private void deleteDataForLogin(String delName, String sql, int delLimit) {
        long totalDelCount = 0;
        long startTime = System.currentTimeMillis();
        Random rnd = new Random();
        try {
            sql = sql + " limit " + delLimit;
            startTime = System.currentTimeMillis();
            int delCount = SqlDao.getInstance().delete(sql);
            LogUtil.i("clearData|TaskManager|1|" + delName + "|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime);

            totalDelCount = delCount;
            while (delCount == delLimit) {
                int randTimeInterval = rnd.nextInt(del_time_interval);
                Thread.sleep(randTimeInterval);
                startTime = System.currentTimeMillis();
                delCount = SqlDao.getInstance().delete(sql);
                LogUtil.i("clearData|TaskManager|1|" + delName + "|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime);
                totalDelCount += delCount;
            }
            LogUtil.i("clearData|TaskManager|2|" + delName + "|" + totalDelCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|" + sql);
        } catch (Exception e) {
            LogUtil.i("clearData|TaskManager|2|" + delName + "|" + totalDelCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|" + sql);
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 清理回放日志，5天
     * user_play_log表在1kz库
     */
    private void clearUserPlayLog() {
        try {

            String clearDate = TimeUtil.formatXxDays_00(-5);

            String delName = "clearUserPlayLog";
            String delSql = "delete from user_playlog where time < " + "'" + clearDate + "'";
            deleteDataFor1kz(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 玩家房卡消耗/获得日志30天过期清理
     * user_card_record表在1kz库
     */
    private void clearUserCardRecord() {
        try {
            String clearDate = TimeUtil.formatXxDays_00(-15);

            String delName = "clearUserCardRecord";
            String delSql = "delete from user_card_record where createTime < " + "'" + clearDate + "'";
            deleteDataFor1kz(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 清理1kz库的数据
     *
     * @param delName  用于日志区分清理功能
     * @param sql      执行的sql
     * @param delLimit 单次最大清理数据量
     */
    private void deleteDataFor1kz(String delName, String sql, int delLimit) {
        long totalDelCount = 0;
        long startTime = System.currentTimeMillis();
        Random rnd = new Random();
        try {
            sql = sql + " limit " + delLimit;
            startTime = System.currentTimeMillis();
            int delCount = TableLogDao.getInstance().deleteSql(sql);
            LogUtil.i("clearData|TaskManager|1|" + delName + "|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime);

            totalDelCount = delCount;
            while (delCount == delLimit) {
                int randTimeInterval = rnd.nextInt(del_time_interval);
                Thread.sleep(randTimeInterval);
                startTime = System.currentTimeMillis();
                delCount = TableLogDao.getInstance().deleteSql(sql);
                LogUtil.i("clearData|TaskManager|1|" + delName + "|" + delCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime);
                totalDelCount += delCount;
            }
            LogUtil.i("clearData|TaskManager|2|" + delName + "|" + totalDelCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|" + sql);
        } catch (Exception e) {
            LogUtil.i("clearData|TaskManager|2|" + delName + "|" + totalDelCount + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|" + sql);
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 刷新每日下分经验限制
     */
    private void refreshGroupLevelData() {
        try {
            try {
                long startTime = System.currentTimeMillis();
                String sql = " update t_group set creditExpToday = 0 , refreshTimeDaily = now() ;";
                int count = SqlDao.getInstance().update(sql);
                LogUtil.i("clearData|TaskManager|1|refreshGroupLevelData|" + count + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|sql=" + sql);

                startTime = System.currentTimeMillis();
                sql = " update t_group_user set creditExpToday = 0 , refreshTimeDaily = now() ;";
                count = SqlDao.getInstance().update(sql);
                LogUtil.i("clearData|TaskManager|12|refreshGroupLevelData|" + count + "|" + (System.currentTimeMillis() - startTime) + "|" + startTime + "|sql=" + sql);
            } catch (Exception e) {
                LogUtil.e("Exception:" + e.getMessage(), e);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * login库中表数据满足sql条件的最大keyId
     *
     * @param maxKeyIdSql
     * @return
     */
    private long loadMaxKeyIdForLogin(String maxKeyIdSql) {
        LogUtil.i("clearData|TaskManager|loadMaxKeyIdForLogin|" + maxKeyIdSql);
        return SqlDao.getInstance().loadMaxKeyId(maxKeyIdSql);
    }


    private void clearLogGroupTable() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";
        try {
            // ----------------------------- t_table_record ----------------------------------
            delName = "clearLogGroupTable";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from log_group_table where dataDate < " + currentDate + "";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from log_group_table where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearLogGroupCommission() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";
        try {
            // ----------------------------- t_table_record ----------------------------------
            delName = "clearLogGroupCommission";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from log_group_commission where dataDate < " + currentDate + "";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from log_group_commission where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearBjdDataStatistics() {
        String startDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        try {
            String delSql;
            String delName = "clearBjdDataStatistics";
            delSql = "delete from bjd_data_statistics where dataDate <= " + startDate;
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearLoginData() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- t_login_data ----------------------------------
            delName = "clearLoginData";
            delSql = "delete from t_login_data where currentDate <= " + currentDate + "";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearLogGroupUserAlert() {
        String currentDate = TimeUtil.formatXxDays_00(-15);

        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";
        try {
            // ----------------------------- log_group_user_alert ----------------------------------
            delName = "clearLogGroupUserAlert";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from log_group_user_alert where createdTime < '" + currentDate + "'";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from log_group_user_alert where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 清理t_user_statistics表
     */
    private void clearUserStatistics() {
        SimpleDateFormat ymd = new SimpleDateFormat("yyyyMMdd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -60);
        String startDate = ymd.format(cal.getTime());
        String delName = "";
        String maxKeyIdSql = "";
        long maxKeyId = 0;
        String delSql = "";
        try {
            // ----------------------------- t_user_statistics ----------------------------------
            delName = "clearUserStatistics";
            maxKeyIdSql = " select COALESCE( MAX(keyId),0 ) from t_user_statistics where currentDate < " + startDate + "";
            maxKeyId = loadMaxKeyIdForLogin(maxKeyIdSql);
            if (maxKeyId > 0) {
                delSql = "delete from t_user_statistics where keyId <= " + maxKeyId;
                deleteDataForLogin(delName, delSql, delLimit);
            }
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearOnlineData() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMddHHmm");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- t_online_data ----------------------------------
            delName = "clearOnlineData";
            delSql = "delete from t_online_data where currentTime <= " + currentDate + "";
            deleteDataForLogin(delName, delSql, delLimit);



            // ----------------------------- online_data ----------------------------------
            currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

            delName = "clearOnlineData";
            delSql = "delete from online_data where dateTime <= " + currentDate + "";
            deleteDataForLogin(delName, delSql, delLimit);

        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearFirstmyth() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- user_firstmyth ----------------------------------
            delName = "clearFirstmyth";
            delSql = "delete from user_firstmyth where recordDate <= " + currentDate;
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearSystemUserCountLogin() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- system_user_countlogin ----------------------------------
            delName = "clearSystemUserCountLogin";
            delSql = "delete from system_user_countlogin where logintime <= " + currentDate;
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearUserMessage() {
        String currentDate = TimeUtil.formatXxDays_00(-5);

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- user_message ----------------------------------
            delName = "clearUserMessage";
            delSql = "delete from user_message where time <= '" + currentDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearUserShare() {
        String currentDate = TimeUtil.formatXxDays_00(-5);

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- user_share ----------------------------------
            delName = "clearUserShare";
            delSql = "delete from user_share where shareDate  <= '" + currentDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearUserDataStatistics() {
        String currentDate = TimeUtil.formatXxDays_00(-15, "yyyyMMdd");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- t_user_statistics ----------------------------------
            delName = "clearUserDataStatistics";
            delSql = "delete from t_user_statistics where currentDate  <= " + currentDate;
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearItemExchange() {
        String currentDate = TimeUtil.formatXxDays_00(-7);

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- t_item_exchange ----------------------------------
            delName = "clearItemExchange";
            delSql = "delete from t_item_exchange where createdTime <=  '" + currentDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearGroupReview() {
        String currentDate = TimeUtil.formatXxDays_00(-7);

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- t_group_review ----------------------------------
            delName = "clearGroupReview";
            delSql = "delete from t_group_review where createdTime <=  '" + currentDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearStatisticsPlatform() {
        String currentDate = TimeUtil.formatXxDays_00(-7, "yyyyMMdd");

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- statistics_platform ----------------------------------
            delName = "clearStatisticsPlatform";
            delSql = "delete from statistics_platform where dateTime <=  " + currentDate;
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearLogActivityReward() {
        String currentDate = TimeUtil.formatXxDays_00(-14);

        String delName = "";
        String delSql = "";

        try {
            // ----------------------------- log_activity_reward ----------------------------------
            delName = "clearLogActivityReward";
            delSql = "delete from log_activity_reward where activityType in(-1000,-1001,-1002,-1003,-1004) and createdTime <=  '" + currentDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearRoomcardConsumeStatistics() {
        String clearDate = TimeUtil.formatXxDays_00(-15, "yyyy-MM-dd");
        String delName = "";
        String delSql = "";
        try {
            // ----------------------------- roomcard_consume_statistics ----------------------------------
            delName = "clearRoomcardConsumeStatistics";
            delSql = "delete from roomcard_consume_statistics where consumeDate <= '" + clearDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearRoomgoldConsumeStatistics() {
        String clearDate = TimeUtil.formatXxDays_00(-15, "yyyy-MM-dd");
        String delName = "";
        String delSql = "";
        try {
            // ----------------------------- roomgold_consume_statistics ----------------------------------
            delName = "clearRoomgoldConsumeStatistics";
            delSql = "delete from roomgold_consume_statistics where consumeDate <= '" + clearDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

    private void clearRedbagRainResult(){
        String clearDate = LocalDate.now().plusDays(-16).toString();
        String delName = "";
        String delSql = "";
        try {
            // ----------------------------- clearRedbagRainResult ----------------------------------
            delName = "clearRedbagRainResult";
            delSql = "delete from t_group_redBagRainResult where pushendtime <= '" + clearDate + "'";
            deleteDataForLogin(delName, delSql, delLimit);
        } catch (Exception e) {
            LogUtil.e("Exception:" + e.getMessage(), e);
        }
    }

}
