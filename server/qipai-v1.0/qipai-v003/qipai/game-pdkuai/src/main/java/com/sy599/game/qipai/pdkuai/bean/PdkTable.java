package com.sy599.game.qipai.pdkuai.bean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sy.mainland.util.CommonUtil;
import com.sy599.game.GameServerConfig;
import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.bean.CreateTableInfo;
import com.sy599.game.common.constant.FirstmythConstants;
import com.sy599.game.common.constant.LangMsg;
import com.sy599.game.common.constant.SharedConstants;
import com.sy599.game.common.constant.SharedConstants.player_state;
import com.sy599.game.common.constant.SharedConstants.table_state;
import com.sy599.game.db.bean.DataStatistics;
import com.sy599.game.db.bean.PdkRateConfig;
import com.sy599.game.db.bean.TableInf;
import com.sy599.game.db.bean.UserPlaylog;
import com.sy599.game.db.dao.DataStatisticsDao;
import com.sy599.game.db.dao.PdkRateConfigDao;
import com.sy599.game.db.dao.TableDao;
import com.sy599.game.db.dao.TableLogDao;
import com.sy599.game.db.dao.UserDatasDao;
import com.sy599.game.jjs.bean.MatchBean;
import com.sy599.game.jjs.util.JjsUtil;
import com.sy599.game.robot.RobotManager;
import com.sy599.game.manager.TableManager;
import com.sy599.game.msg.serverPacket.ComMsg;
import com.sy599.game.msg.serverPacket.ComMsg.ComRes;
import com.sy599.game.msg.serverPacket.PlayCardResMsg.PlayCardRes;
import com.sy599.game.msg.serverPacket.TableRes.ClosingInfoRes;
import com.sy599.game.msg.serverPacket.TableRes.ClosingPlayerInfoRes;
import com.sy599.game.msg.serverPacket.TableRes.CreateTableRes;
import com.sy599.game.msg.serverPacket.TableRes.DealInfoRes;
import com.sy599.game.msg.serverPacket.TableRes.PlayerInTableRes;
import com.sy599.game.qipai.pdkuai.constant.PdkConstants;
import com.sy599.game.qipai.pdkuai.robot.btlibrary.PaodekuaiBTLibrary;
import com.sy599.game.qipai.pdkuai.tool.CardTool;
import com.sy599.game.qipai.pdkuai.tool.CardTypeTool;
import com.sy599.game.qipai.pdkuai.tool.Scheme;
import com.sy599.game.qipai.pdkuai.util.CardUtils;
import com.sy599.game.udplog.UdpLogger;
import com.sy599.game.util.GameConfigUtil;
import com.sy599.game.util.GameUtil;
import com.sy599.game.util.JacksonUtil;
import com.sy599.game.util.JsonWrapper;
import com.sy599.game.util.LangHelp;
import com.sy599.game.util.LogUtil;
import com.sy599.game.util.PayConfigUtil;
import com.sy599.game.util.ResourcesConfigsUtil;
import com.sy599.game.util.SendMsgUtil;
import com.sy599.game.util.StringUtil;
import com.sy599.game.util.TimeUtil;
import com.sy599.game.websocket.constant.WebSocketMsgType;

import jbt.execution.core.BTExecutorFactory;
import jbt.execution.core.ContextFactory;
import jbt.execution.core.IBTExecutor;
import jbt.execution.core.IBTLibrary;
import jbt.execution.core.IContext;
import jbt.execution.core.ExecutionTask.Status;
import jbt.model.core.ModelTask;

import org.apache.commons.lang3.StringUtils;

public class PdkTable extends BaseTable {
    public static final String GAME_CODE = "pdk";
    private static final int JSON_TAG = 1;
    /*** ???????????????????????? */
    private volatile List<Integer> nowDisCardIds = new ArrayList<>();
    /*** ??????map */
    private Map<Long, PdkPlayer> playerMap = new ConcurrentHashMap<Long, PdkPlayer>();
    /*** ????????????????????? */
    private Map<Integer, PdkPlayer> seatMap = new ConcurrentHashMap<Integer, PdkPlayer>();
    /*** ?????????????????? */
    private volatile int max_player_count = 3;

    private volatile int isFirstRoundDisThree;// ????????????????????????

    public static final int FAPAI_PLAYER_COUNT = 3;// ????????????

    private volatile List<Integer> cutCardList = new ArrayList<>();// ????????????

    private volatile int showCardNumber = 0; // ???????????????????????????

    private volatile int redTen;//?????????10  1:5???  2:10??? 3:??????
    private volatile int siDai;//?????? 0?????? 1???1 2???2 3???3
    private volatile int isFirstCardType32;//???????????????????????????3???2
    private volatile int card3Eq = 1;//??????/?????????????????????(???????????????????????????????????????????????????)0???1??????????????????????????????

    private volatile int timeNum = 0;

    //???????????????3??????2???pass???????????????????????????
    private boolean newRound = true;
    //pass??????
    private volatile int passNum=0;
    /**
     * ????????????
     */
    private volatile int autoTimeOut = 5 * 24 * 60 * 60 * 1000;
    private volatile int autoTimeOut2 = 5 * 24 * 60 * 60 * 1000;

    //??????????????????
    private int finishFapai=0;

    //???????????????0??????1???
    private int jiaBei;
    //?????????????????????xx???????????????
    private int jiaBeiFen;
    //????????????????????????
    private int jiaBeiShu;
    //?????????????????????
    private int isNoBoom;

    /**??????1????????????2?????????*/
    private int autoPlayGlob;
    private int autoTableCount;
    //??????????????????
    private int isPlayBack;
    //????????????????????????????????????????????????????????????
    private volatile List<PlayerCard> noPassDisCard = new ArrayList<>();
    //????????????
    private volatile String replayDisCard = "";

    /** ???????????????0????????? ???????????????1-100**/
    private int daNiaoFen;

    /** ?????????????????????0??????1???**/
    private int chai4Zha;
    /** 3???A????????????0??????1???**/
    private int AAAZha;

    /** ???????????? 1??????**/
    private int tableStatus = 0;

    //?????? 0:?????? 1:?????????1 2:?????????2 3:???123  4:???235  5:???258
    private int piaoFenType=0;
    //??????????????????????????????
    private int isSendPiaoFenMsg=0;
    //??????below??????
    private int belowAdd=0;
    private int below=0;

    public int getCard3Eq() {
        return card3Eq;
    }

    public void setCard3Eq(int card3Eq) {
        this.card3Eq = card3Eq;
        changeExtend();
    }

    public int getIsFirstCardType32() {
        return isFirstCardType32;
    }

    public void setIsFirstCardType32(int isFirstCardType32) {
        this.isFirstCardType32 = isFirstCardType32;
        changeExtend();
    }

    public String getReplayDisCard() {
        return replayDisCard;
    }

    public void setReplayDisCard(String replayDisCard) {
        this.replayDisCard = replayDisCard;
    }

    public int getSiDai() {
        return siDai;
    }

    public void setSiDai(int siDai) {
        this.siDai = siDai;
        changeExtend();
    }

    public int getRedTen() {
        return redTen;
    }

    public void setRedTen(int redTen) {
        this.redTen = redTen;
        changeExtend();
    }

    public int getPiaoFenType() {
        return piaoFenType;
    }

    public void setPiaoFenType(int piaoFenType) {
        this.piaoFenType = piaoFenType;
    }

    @Override
    protected void loadFromDB1(TableInf info) {
        if (!StringUtils.isBlank(info.getNowDisCardIds())) {
            this.nowDisCardIds = StringUtil.explodeToIntList(info.getNowDisCardIds());
        }
        if (isMatchRoom()) {
            autoTimeOut = ResourcesConfigsUtil.loadIntegerValue("ServerConfig", "matchAutoTimeOutPdk", 15 * 1000);
        }else{
//            autoTimeOut = ResourcesConfigsUtil.loadIntegerValue("ServerConfig", "autoTimeOutPdkNormal", 30 * 1000);
//            autoTimeOut2 = ResourcesConfigsUtil.loadIntegerValue("ServerConfig", "autoTimeOutPdkNormal2", 20 * 1000);
        }
    }

    public long getId() {
        return id;
    }

    public PdkPlayer getPlayer(long id) {
        return playerMap.get(id);
    }

    /**
     * ????????????
     */
    public void calcOver() {
        PdkPlayer winPlayer = null;
        int winPoint = 0;
        Map<Integer, Integer> lossPoint = new HashMap<>();
        int closeNum = 0;
        int shangYouSeat = 0; // ???????????????
        for (PdkPlayer player : seatMap.values()) {
            player.changeState(player_state.over);
            int left = player.getHandPais().size();

            int currentLs = player.getCurrentLs();
            int maxLs = player.getMaxLs();
            if (left == 0) {
                shangYouSeat = player.getSeat();
                winPlayer = player;
                currentLs++;
                player.setCurrentLs(currentLs);
                UserDatasDao.getInstance().updateUserDatas(String.valueOf(player.getUserId()), GAME_CODE, "all", "currentLs", String.valueOf(currentLs));
                if (currentLs > maxLs) {
                    maxLs = currentLs;
                    player.setMaxLs(maxLs);
                    UserDatasDao.getInstance().updateUserDatas(String.valueOf(player.getUserId()), GAME_CODE, "all", "maxLs", String.valueOf(maxLs));
                }
                player.setCurrentLshu(0);
            } else {
                if (currentLs > 0) {
                    currentLs = 0;
                    player.setCurrentLs(currentLs);
                    UserDatasDao.getInstance().updateUserDatas(String.valueOf(player.getUserId()), GAME_CODE, "all", "currentLs", String.valueOf(currentLs));
                }
                player.setCurrentLshu(player.getCurrentLshu()+1);

                // ????????????????????????1???????????????
                int point = (left <= 1&&!isGoldRoom()) ? 0 : left;
                if (!player.isOutCards()) {
                    // ???????????????????????????
                    point = point * 2;
                    closeNum++;
                    player.getMyExtend().setPdkFengshen(FirstmythConstants.firstmyth_index6, 1);
                }
                lossPoint.put(player.getSeat(), point);
            }
        }
        if (winPlayer == null) {
            return;
        }
        if (closeNum > 0) {
            winPlayer.getMyExtend().setPdkFengshen(FirstmythConstants.firstmyth_index1, closeNum);
        }

        for (Map.Entry<Integer, Integer> entry : lossPoint.entrySet()) {
            PdkPlayer player = seatMap.get(entry.getKey());
            int point0 = entry.getValue();
            int point = calcRedTen(winPlayer, player, point0);
            if (point != point0) {
                entry.setValue(point);
            }
            winPoint += point;
        }

        for (Map.Entry<Integer, Integer> entry : lossPoint.entrySet()) {
            seatMap.get(entry.getKey()).calcLost(this, 1, -entry.getValue());
        }
        winPlayer.calcWin(this, 1, winPoint);

        // ------------?????????------------
        if (piaoFenType > 0) {
            for (PdkPlayer p : seatMap.values()) {
                if (winPlayer.getUserId() != p.getUserId()) {
                    setWinLostPiaoFen(winPlayer, p);
                }
            }
            for (PdkPlayer p : seatMap.values()) {
                p.setPoint(p.getPoint() + p.getWinLostPiaoFen());
                p.changeTotalPoint(p.getWinLostPiaoFen());
            }
        }

        boolean isOver = playBureau >= totalBureau;
        if (autoPlayGlob > 0) {
            //????????????
            boolean diss = false;
            if (autoPlayGlob == 1) {
                for (PdkPlayer seat : seatMap.values()) {
                    if (seat.isAutoPlay()) {
                        diss = true;
                        break;
                    }

                }
            } else if (autoPlayGlob == 3) {
                diss = checkAuto3();
            }
            if (diss) {
                autoPlayDiss = true;
                isOver = true;
            }
        }

        if(isOver){
            calcPointBeforeOver();
        }

        // -----------?????????---------------------------------
        if (isGoldRoom()) {
            for (PdkPlayer player : seatMap.values()) {
                player.setPoint(player.getTotalPoint());
                player.setWinGold(player.getTotalPoint());
                if (player.getPlayBoomPoint() != 0) {
                    player.setPlayBoomPoint((int) (player.getPlayBoomPoint() * goldRoom.getRate()));
                }
                if (player.getWinLostPiaoFen() != 0) {
                    player.setWinLostPiaoFen((int) (player.getWinLostPiaoFen() * goldRoom.getRate()));
                }
            }
            calcGoldRoom();
        }

        // -----------solo------------------
        if (isSoloRoom()) {
            if (shangYouSeat != 0) {
                for (PdkPlayer player : seatMap.values()) {
                    if (player.getSeat() == shangYouSeat) {
                        player.setSoloWinner(true);
                    } else {
                        player.setSoloWinner(false);
                    }
                }
                calcSoloRoom();
            }
        }

        // --------------?????????
		if(isCompetitionRoom()) {
			//????????????
			Iterator<PdkPlayer> iterator = seatMap.values().iterator();
			while (iterator.hasNext()) {
				PdkPlayer next = iterator.next();

//				System.out.println(next.getName() + " ?????? " + next.getPoint() + " ?????? " + getCompetitionRoom().getRate());
//				System.out.println(next.getName() + " ????????? " + next.getTotalPoint());

				int pointTemp = next.getPoint();
				next.setPoint(next.getPoint() * (int) getCompetitionRoom().getSecondsRate());
				//???????????????????????????????????????
				next.setTotalPoint(next.getTotalPoint() - pointTemp);
				//????????????????????????
				next.setTotalPoint(next.getTotalPoint() + next.getPoint());
//
//				System.out.println(next.getName() + " ?????? " + next.getPoint() + " ?????? " + getCompetitionRoom().getRate());
//				System.out.println(next.getName() + " ????????? " + next.getTotalPoint());
			}
		}


        calcAfter();
        ClosingInfoRes.Builder res = sendAccountsMsg(isOver, winPlayer, false);
        saveLog(isOver, winPlayer.getUserId(), res.build());
        setLastWinSeat(winPlayer.getSeat());
        if (isOver) {
            calcOver1();
            calcOver2();
            calcOver3();
            diss();

            for (Player player : seatMap.values()) {
                player.saveBaseInfo();
            }
        } else {
            initNext();
            calcOver1();

            for (Map.Entry<Integer, Player> kv : getSeatMap().entrySet()) {
                int seat = kv.getKey().intValue();
                if (seat != kv.getValue().getSeat()) {
                    LogUtil.errorLog.warn("table user seat error3:tableId={},userId={},seat={},auto change seat={}", id, kv.getValue().getUserId(), kv.getValue().getSeat(), seat);

                    kv.getValue().setSeat(seat);
                    kv.getValue().setPlayingTableId(id);
                    changePlayers();
                }
                kv.getValue().saveBaseInfo();
            }
        }

    }

    public void calcPointBeforeOver(){
        // --------------????????????------------
        if (daNiaoFen > 0) {
            for (PdkPlayer winner : seatMap.values()) {
                for (PdkPlayer loser : seatMap.values()) {
                    if (loser.getSeat() != winner.getSeat() && winner.getTotalPoint() > loser.getTotalPoint()) {
                        int niaoFen = (winner.getNiaoFen() + loser.getNiaoFen());
                        winner.setTotalPoint(winner.getTotalPoint() + niaoFen);
                        loser.setTotalPoint(loser.getTotalPoint() - niaoFen);
                    }
                }
            }
        }

        // ------------????????????????????????------------
        if (jiaBei == 1) {
            int jiaBeiPoint = 0;
            int loserCount = 0;
            for (PdkPlayer player : seatMap.values()) {
                if (player.getTotalPoint() > 0 && player.getTotalPoint() < jiaBeiFen) {
                    jiaBeiPoint += player.getTotalPoint() * (jiaBeiShu - 1);
                    player.setTotalPoint(player.getTotalPoint() * jiaBeiShu);
                } else if (player.getTotalPoint() < 0) {
                    loserCount++;
                }
            }
            if (jiaBeiPoint > 0) {
                for (PdkPlayer player : seatMap.values()) {
                    if (player.getTotalPoint() < 0) {
                        player.setTotalPoint(player.getTotalPoint() - (jiaBeiPoint / loserCount));
                    }
                }
            }
        }
        // ------------???????????????below???+belowAdd???------------
        if (belowAdd > 0 && playerMap.size() == 2) {
            for (PdkPlayer player : seatMap.values()) {
                int totalPoint = player.getTotalPoint();
                if (totalPoint > -below && totalPoint < 0) {
                    player.setTotalPoint(player.getTotalPoint() - belowAdd);
                } else if (totalPoint < below && totalPoint > 0) {
                    player.setTotalPoint(player.getTotalPoint() + belowAdd);
                }
            }
        }
    }

	private boolean checkAuto3() {
		boolean diss = false;
			boolean diss2 = false;
			 for (PdkPlayer seat : seatMap.values()) {
		      	if(seat.isAutoPlay()) {
		      		diss2 = true;
		          	break;
		      	    }
		      }
			 if(diss2) {
				 autoTableCount +=1;
			 }else{
				 autoTableCount = 0;
			 }
			if(autoTableCount==3) {
				diss = true;
			}
		return diss;
	}

    @Override
    public void calcDataStatistics2() {
        //??????????????? ???????????????????????????????????????????????????????????????????????????????????????????????? ????????????
        if (isGroupRoom()) {
            String groupId = loadGroupId();
            int maxPoint = 0;
            int minPoint = 0;
            Long dataDate = Long.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
            //???????????????????????????
            calcDataStatistics3(groupId);

            //Long dataDate, String dataCode, String userId, String gameType, String dataType, int dataValue
            for (PdkPlayer player : playerMap.values()) {
                //????????????
                DataStatistics dataStatistics1 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "xjsCount", playedBureau);
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics1, 3);
                //????????????
                DataStatistics dataStatistics5 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "djsCount", 1);
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics5, 3);
                //?????????
                DataStatistics dataStatistics6 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "zjfCount", player.loadScore());
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics6, 3);
                if (player.loadScore() > 0) {
                    if (player.loadScore() > maxPoint) {
                        maxPoint = player.loadScore();
                    }
                    //??????????????????
                    DataStatistics dataStatistics2 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "winMaxScore", player.loadScore());
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics2, 4);
                } else if (player.loadScore() < 0) {
                    if (player.loadScore() < minPoint) {
                        minPoint = player.loadScore();
                    }
                    //??????????????????
                    DataStatistics dataStatistics3 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "loseMaxScore", player.loadScore());
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics3, 5);
                }
            }

            for (PdkPlayer player : playerMap.values()) {
                if (maxPoint > 0 && maxPoint == player.loadScore()) {
                    //??????????????????
                    DataStatistics dataStatistics4 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "dyjCount", 1);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics4, 1);
                } else if (minPoint < 0 && minPoint == player.loadScore()) {
                    //??????????????????
                    DataStatistics dataStatistics5 = new DataStatistics(dataDate, "group" + groupId, String.valueOf(player.getUserId()), String.valueOf(playType), "dfhCount", 1);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics5, 2);
                }
            }
        }
    }

    public int calcRedTen(PdkPlayer winPlayer, PdkPlayer player, int point) {
        if (this.getRedTen() > 0 && (winPlayer.getRedTenPai() == 1 || player.getRedTenPai() == 1)) {
            switch (this.getRedTen()) {
                case 1:
                    point += 5;
                    break;
                case 2:
                    point += 10;
                    break;
                case 3:
                    point *= 2;
                    break;
            }
        }
        return point;
    }


    public void saveLog(boolean over, long winId, Object resObject) {
        ClosingInfoRes res = (ClosingInfoRes) resObject;
        LogUtil.d_msg("tableId:" + id + " play:" + playBureau + " over:" + res);
        String logRes = JacksonUtil.writeValueAsString(LogUtil.buildClosingInfoResLog(res));
        String logOtherRes = JacksonUtil.writeValueAsString(LogUtil.buildClosingInfoResOtherLog(res));
        Date now = TimeUtil.now();

        UserPlaylog userLog = new UserPlaylog();
        userLog.setUserId(creatorId);
        userLog.setLogId(playType);
        userLog.setTableId(id);
        userLog.setRes(extendLogDeal(logRes));
        userLog.setTime(now);
        userLog.setTotalCount(totalBureau);
        userLog.setCount(playBureau);
        userLog.setStartseat(lastWinSeat);
        userLog.setOutCards(playLog);
        userLog.setExtend(logOtherRes);
        userLog.setMaxPlayerCount(max_player_count);
        userLog.setType(creditMode == 1 ? 2 : 1 );
        userLog.setGeneralExt(buildGeneralExtForPlaylog().toString());
        long logId = TableLogDao.getInstance().save(userLog);
        saveTableRecord(logId, over, playBureau);

        if (!isGoldRoom()) {
            for (PdkPlayer player : playerMap.values()) {
                player.addRecord(logId, playBureau);
            }
        }

        UdpLogger.getInstance().sendSnapshotLog(masterId, playLog, logRes);
    }

    public Map<String, Object> saveDB(boolean asyn) {
        if (id < 0) {
            return null;
        }

        Map<String, Object> tempMap = loadCurrentDbMap();
        if (!tempMap.isEmpty()) {
            tempMap.put("tableId", id);
            tempMap.put("roomId", roomId);
            if (tempMap.containsKey("players")) {
                tempMap.put("players", buildPlayersInfo());
            }
            if (tempMap.containsKey("outPai1")) {
                tempMap.put("outPai1", StringUtil.implodeLists(seatMap.get(1).getOutPais()));
            }
            if (tempMap.containsKey("outPai2")) {
                tempMap.put("outPai2", StringUtil.implodeLists(seatMap.get(2).getOutPais()));
            }
            if (tempMap.containsKey("outPai3")) {
                tempMap.put("outPai3", StringUtil.implodeLists(seatMap.get(3).getOutPais()));
            }
            if (tempMap.containsKey("handPai1")) {
                tempMap.put("handPai1", StringUtil.implode(seatMap.get(1).getHandPais(), ","));
            }
            if (tempMap.containsKey("handPai2")) {
                tempMap.put("handPai2", StringUtil.implode(seatMap.get(2).getHandPais(), ","));
            }
            if (tempMap.containsKey("handPai3")) {
                tempMap.put("handPai3", StringUtil.implode(seatMap.get(3).getHandPais(), ","));
            }
            if (tempMap.containsKey("answerDiss")) {
                tempMap.put("answerDiss", buildDissInfo());
            }
            if (tempMap.containsKey("nowDisCardIds")) {
                tempMap.put("nowDisCardIds", StringUtil.implode(nowDisCardIds, ","));
            }
            if (tempMap.containsKey("extend")) {
                tempMap.put("extend", buildExtend());
            }
//			TableDao.getInstance().save(tempMap);
        }
        return tempMap.size() > 0 ? tempMap : null;
    }

    @Override
    public JsonWrapper buildExtend0(JsonWrapper wrapper) {
//		JsonWrapper wrapper = new JsonWrapper("");
        wrapper.putInt(1, isFirstRoundDisThree);
        wrapper.putInt(2, max_player_count);
        wrapper.putInt(3, showCardNumber);
        wrapper.putInt(4, redTen);
        wrapper.putInt(5, siDai);
        wrapper.putInt(6, isFirstCardType32);
        wrapper.putString("card3Eq", String.valueOf(card3Eq));
//		return wrapper.toString();
        wrapper.putInt(7, jiaBei);
        wrapper.putInt(8, jiaBeiFen);
        wrapper.putInt(9, jiaBeiShu);
        wrapper.putInt(10,isNoBoom);

        wrapper.putInt(11,isPlayBack);
        wrapper.putString(12,replayDisCard);
        wrapper.putInt(13,autoTimeOut);
        wrapper.putInt(14,autoPlayGlob);
        wrapper.putInt(15,daNiaoFen);
        wrapper.putInt(16,chai4Zha);
        wrapper.putInt(17, newRound ? 1 : 0);
        wrapper.putInt(18, AAAZha);
        wrapper.putInt(19, piaoFenType);
        wrapper.putInt(20, isSendPiaoFenMsg);
        wrapper.putInt(21, finishFapai);
        wrapper.putInt(22, belowAdd);
        wrapper.putInt(23, below);
        return wrapper;
    }



    public void changePlayers() {
        dbParamMap.put("players", JSON_TAG);
    }

    public void changeCards(int seat) {
        dbParamMap.put("outPai" + seat, JSON_TAG);
        dbParamMap.put("handPai" + seat, JSON_TAG);
    }

    /**
     * ????????????
     */
    public void fapai() {
        synchronized (this) {
            changeTableState(table_state.play);
            timeNum = 0;
        	PdkPlayer winPlayer = null;
        	PdkPlayer losePlayer = null;
        	List<PdkRateConfig> configs = new ArrayList<>();
			for (PdkPlayer player : playerMap.values()) {
				PdkRateConfig config = getPdkRateConfig(player);
				if (config == null) {
					continue;
				}
				if (config.getType() <= 3) {
					if (winPlayer == null)
						winPlayer = player;
				} else {
					if (losePlayer == null)
						losePlayer = player;
				}
				configs.add(config);
			}
            
            if(!configs.isEmpty()){
            	 sysPeiPai(winPlayer, losePlayer, configs);
            }else{
            	 commonFaPai();
            }
        	setTableStatus(0);
        }
        finishFapai=1;
    }

	private void sysPeiPai(PdkPlayer winPlayer, PdkPlayer losePlayer, List<PdkRateConfig> configs) {
		List<List<Integer>> list =  CardTool.fapaiPingHeng(max_player_count, playType, configs, isNoBoom==1,zp);
		if(losePlayer!=null){
			losePlayer.dealHandPais(list.get(0), this);
			list.remove(0);
			PdkRateConfig config = CardTool.getXuanZCongfig(configs, 4);
			if(config!=null){
				faPaiGamLog(losePlayer,config.getType());
			}
		}
		if(winPlayer!=null){
			winPlayer.dealHandPais(list.get(0), this);
			list.remove(0);
			PdkRateConfig config = CardTool.getXuanZCongfig(configs, 1);
			if(config!=null){
				faPaiGamLog(winPlayer,config.getType());
			}
		}
		
		int i = 0;
		for (PdkPlayer player : playerMap.values()) { 
		    player.changeState(player_state.play);
		    if(player.getHandPais().isEmpty()){
		    	player.dealHandPais(list.get(i), this);
		    	i++;
		    	 faPaiGamLog(player,0);
		    }
		    if (getRedTen() > 0 && player.getHandPais().contains(310)) {//???????????????10
		        player.setRedTenPai(1);
		    }
		    player.setIsNoLet(0);

		    if (!player.isAutoPlay()) {
		        player.setAutoPlay(false, this);
		        player.setLastOperateTime(System.currentTimeMillis());
		    }
		   
		}
		//??????
		if (isTwoPlayer()) {
		    this.cutCardList.clear();
		    this.cutCardList.addAll(list.get(i));
		}
	}

	private void faPaiGamLog(PdkPlayer player,int peiType) {
		StringBuilder sb = new StringBuilder("Pdk");
		sb.append("|").append(getId());
		sb.append("|").append(getPlayBureau());
		sb.append("|").append(player.getUserId());
		sb.append("|").append(player.getSeat());
		sb.append("|").append(player.getName());
		sb.append("|").append(player.isAutoPlay() ? 1 : 0);
		if(peiType==0){
			sb.append("|").append("faPai");
		}else{
			sb.append("|").append("peiPai");
		}
		sb.append("|").append(player.getHandPais());
		sb.append("|").append(peiType);
		LogUtil.msgLog.info(sb.toString());
	}
    
    private PdkRateConfig getPdkRateConfig(PdkPlayer player){
    	PdkRateConfig res = null;
    	List<PdkRateConfig> configs =   PdkRateConfigDao.getInstance().queryAllPdkConfig();
    	int bur = getPeiPaiBureau();
    	for(PdkRateConfig  config : configs){
    		if(config.getType()==Scheme.WIN_A&&config.getVal()<=player.getTotalPoint()){
    			res = config;
    		}else if(config.getType()==Scheme.WIN_B&&player.getCurrentLs()>=config.getVal()){
    			res = config;
    		}else if(config.getType()==Scheme.WIN_C&&player.getWinCount()>=bur){
    			res = config;
    		}else if(config.getType()==Scheme.LOSE_A&&config.getVal()<=-player.getTotalPoint()){
    			res = config;
    		}else if(config.getType()==Scheme.LOSE_B&&player.getCurrentLshu()>=config.getVal()){
    			res = config;
    		}else if(config.getType()==Scheme.LOSE_C&&player.getLostCount()>=bur){
    			res = config;
    		}
    		
    	}
    	return res;
    	
    }
    
    private int getPeiPaiBureau(){
    	int rate = totalBureau/10;
    	if(rate==0){
    		rate=1;
    	}
    	int bur = totalBureau/max_player_count+rate;
    	
    	return bur;
    	
    }

	private void commonFaPai() {
		List<List<Integer>> list;
		if(isNoBoom==1){
		    list = CardTool.fapaiNoBoom(max_player_count, playType, zp);
		}else {
		    list = CardTool.fapai(max_player_count, playType, zp);
		}
		
		int i = 0;
		for (PdkPlayer player : playerMap.values()) {
		    player.changeState(player_state.play);
		    player.dealHandPais(list.get(i), this);
		    if (getRedTen() > 0 && list.get(i).contains(310)) {//???????????????10
		        player.setRedTenPai(1);
		    }
		    player.setIsNoLet(0);
		    i++;

		    if (!player.isAutoPlay()) {
		        player.setAutoPlay(false, this);
		        player.setLastOperateTime(System.currentTimeMillis());
		    }
		    faPaiGamLog(player,0);
		}
		if (isTwoPlayer()) {
		    this.cutCardList.clear();
		    this.cutCardList.addAll(list.get(i));
		}
	}




    /**
     * ??????????????????seat
     *
     * @return
     */
    public int getNextDisCardSeat() {
        int seat = 0;
        if (state != table_state.play) {
            return seat;
        }
        if (disCardRound == 0) {
            if (lastWinSeat == 0) {
                // ?????????????????? ?????????3????????????
                for (PdkPlayer player : playerMap.values()) {
                	
                	
                	if(getPlayType() == GameUtil.play_type_11){
                		 if (player.getHandPais().contains(406)) {
                             seat = player.getSeat();
                             return seat;
                         }
                	}else{
                		 if (player.getHandPais().contains(403)) {
                             seat = player.getSeat();
                             return seat;
                         }
                	}
                   
                }
                // ?????????3??????????????????
                if (0 == seat) {
                    for (int temp : seatMap.keySet()) {
                        if (seat == 0) {
                            seat = temp;
                        } else {
                            if (seat > temp) {
                                seat = temp;
                            }
                        }
                    }
//					seat = RandomUtils.nextInt(2) + 1;
                }
            } else {
                return lastWinSeat;
            }

        } else {
            if (nowDisCardSeat != 0) {
                seat = nowDisCardSeat >= max_player_count ? 1 : nowDisCardSeat + 1;
            }
        }
        return seat;
    }

    public PdkPlayer getPlayerBySeat(int seat) {
        int next = seat >= max_player_count ? 1 : seat + 1;
        return seatMap.get(next);

    }

    public Map<Integer, Player> getSeatMap() {
        Object o = seatMap;
        return (Map<Integer, Player>) o;
    }

    public int getChai4Zha() {
        return chai4Zha;
    }



    @Override
    public CreateTableRes buildCreateTableRes(long userId, boolean isrecover, boolean isLastReady) {
        CreateTableRes.Builder res = CreateTableRes.newBuilder();
        buildCreateTableRes0(res);
        synchronized (this) {
        	res.setTableType(getTableType());
            res.setNowBurCount(getPlayBureau());
            res.setTotalBurCount(getTotalBureau());
            res.setGotyeRoomId(gotyeRoomId + "");
            res.setTableId(getId()+"");
            res.setWanfa(playType);
            List<PlayerInTableRes> players = new ArrayList<>();
            for (PdkPlayer player : playerMap.values()) {
                PlayerInTableRes.Builder playerRes = player.buildPlayInTableInfo(userId, isrecover);
                if (playerRes == null) {
                    continue;
                }
                if (player.getUserId() == userId) {
                    // ????????????????????????????????????
                    playerRes.addAllHandCardIds(player.getHandPais());
                } else {
                    // ?????????????????????????????????????????????????????????????????????
                }

                if (player.getSeat() == disCardSeat && nowDisCardIds != null && nowDisCardIds.size()>0) {
                    playerRes.addAllOutCardIds(nowDisCardIds);
                    int cardType = CardUtils.cardResult2ReturnType(CardUtils.calcCardValue(CardUtils.loadCards(nowDisCardIds),siDai,isFirstCardType32==1,AAAZha==1));
                    playerRes.addRecover(cardType);
                }
                players.add(playerRes.build());
            }
            res.addAllPlayers(players);
            int nextSeat = getNextDisCardSeat();
            if (nextSeat != 0) {
                res.setNextSeat(nextSeat);
            }
            res.setRenshu(this.max_player_count);
            res.addExt(this.showCardNumber);//0
            res.addExt(this.isFirstRoundDisThree);//1
            res.addExt(this.payType);//2
            res.addExt(this.redTen);//3
            res.addExt(this.siDai);//4

            res.addExt(CommonUtil.isPureNumber(modeId) ? Integer.parseInt(modeId) : 0);//5
            int ratio;
            int pay;
            MatchBean matchBean = isMatchRoom() ? JjsUtil.loadMatch(matchId) : null;
            if (matchBean != null) {
                ratio = (int) matchRatio;
                pay = 0;
            } else if (isGoldRoom()) {
                ratio = 0;
                pay = 0;
            } else if(isCompetitionRoom()){
				ratio = (int) matchRatio;
				pay = 0;//PayConfigUtil.get(playType, totalBureau, max_player_count, payType == 1 ? 0 : 1, modeId);
			}else {
                ratio = 1;
                pay = consumeCards() ? loadPayConfig(payType) : 0;
            }

            res.addExt(ratio);//6
            res.addExt(pay);//7
            res.addExt(lastWinSeat);//8
            if (matchBean != null) {
                int num = JjsUtil.loadMatchCurrentGameNo(matchBean);
                res.addExt(num);//9
                res.addExt(num == 0 ? JjsUtil.loadMinScore(matchBean, getMatchRatio()) : 0);//10
            } else if(isCompetitionRoom()) {
				res.addExt(getCompetitionRoom().getWeedOutScore());
				res.addExt((int) getCompetitionRoom().getRate());
            } else {
                res.addExt(0);
                res.addExt(0);
            }

            if(isCompetitionRoom()) {
				res.addExtStr("");	//getCompetitionRoom().getCreatedTime().getTime() +
				res.addExtStr(cardMarkerToJSON());//1
				res.addExtStr(getCompetitionRoom().getPlayingId() + "");//2
			}else {
				res.addExtStr(String.valueOf(matchId));//0
				res.addExtStr(cardMarkerToJSON());//1
				res.addExtStr("");
			}


            res.addTimeOut((isGoldRoom() || autoPlay) ? autoTimeOut : 0);
            if (matchBean != null) {
                if (disCardRound == 0) {
                    res.addTimeOut((autoTimeOut + 5000));
                } else {
                    res.addTimeOut(autoTimeOut);
                }
            } else if(autoPlay){
                if (disCardRound == 0) {
                    res.addTimeOut((autoTimeOut + 5000));
                } else {
                    res.addTimeOut(autoTimeOut);
                }
            }else{
                res.addTimeOut(0);
            }

            res.addExt(playedBureau);//11
            res.addExt(disCardRound);//12

            res.addExt(card3Eq);//13

            res.addExt(creditMode); //14
//            res.addExt(creditJoinLimit);//15
//            res.addExt(creditDissLimit);//16
//            res.addExt(creditDifen);//17
//            res.addExt(creditCommission);//18
            res.addExt(0);
            res.addExt(0);
            res.addExt(0);
            res.addExt(0);
            res.addExt(creditCommissionMode1);//19
            res.addExt(creditCommissionMode2);//20
            res.addExt(autoPlay ? 1 : 0);//21
            res.addExt(jiaBei); //22
            res.addExt(jiaBeiFen);//23
            res.addExt(jiaBeiShu);//24
            res.addExt(tableStatus);//25
        }

        return res.build();
    }

    public int getOnTablePlayerNum() {
        int num = 0;
        for (PdkPlayer player : seatMap.values()) {
            if (player.getIsLeave() == 0) {
                num++;
            }
        }
        return num;
    }

    public void notLet(PdkPlayer player) {
        // ?????????
        setNowDisCardSeat(player.getSeat());
        player.setIsNoLet(1);
        List<Integer> cards = new ArrayList<>();
        cards.add(0);
        player.addOutPais(cards, this);
        PlayCardRes.Builder res = PlayCardRes.newBuilder();
        res.setUserId(player.getUserId() + "");
        res.setSeat(player.getSeat());
        res.setCardType(0);
        res.setIsPlay(1);
        if (player.getHandPais().size() == 1) {
            // ??????
            res.setIsBt(1);
        }

        for (PdkPlayer pdkPlayer : seatMap.values()) {
            PlayCardRes.Builder copy = res.clone();
            List<Integer> canPlayList = CardTypeTool.canPlay(pdkPlayer.getHandPais(), nowDisCardIds, false, pdkPlayer, this);
            if (disCardSeat == pdkPlayer.getSeat() || !canPlayList.isEmpty()) {
                copy.setIsLet(1);

            }
            pdkPlayer.writeSocket(copy.build());
        }
    }

    /**
     * ??????
     *
     * @param player
     * @param cards
     */
    public void disCards(PdkPlayer player, List<Integer> cards) {
        if (disCardSeat == player.getSeat()) {
            // ???????????????
            clearIsNotLet();
        } else {
            player.setIsNoLet(0);
        }
        if (checkIsChai4Zha(player, cards)) {
            player.writeErrMsg(LangHelp.getMsg(LangMsg.code_70));
            return;
        }
        CardUtils.Result cardResult = CardUtils.calcCardValue(CardUtils.loadCards(cards), siDai,isFirstCardType32==1,AAAZha==1);

        int cardType = -1;
        if (cardResult.getType() == 4 && siDai == 3 && cards.size() == 7 && disCardSeat != player.getSeat()) {
            CardUtils.Result result1 = CardUtils.calcCardValue(CardUtils.loadCards(nowDisCardIds), siDai,isFirstCardType32==1,AAAZha==1);
            if (result1.getType() == 33) {
                cardType = 33;
            }
        }
        if (cardType == 33 || cardResult.getType() == 33) {
            // ??????
            player.getMyExtend().setPdkFengshen(FirstmythConstants.firstmyth_index5, 1);
        }

        cards = CardUtils.loadSortCards(cards, cardResult, siDai,AAAZha==1);

        setDisCardSeat(player.getSeat());
        player.addOutPais(cards, this);
        setNowDisCardIds(cards);
        setNowDisCardSeat(player.getSeat());
        if (cards != null) {
            noPassDisCard.add(new PlayerCard(player.getName(), cards));
        }

        // ??????????????????
        PlayCardRes.Builder res = PlayCardRes.newBuilder();
        res.addAllCardIds(getNowDisCardIds());
        if (isNewRound()) {
            //?????????????????????
//            res.setIsClearDesk(1);
            if (cardResult.getType() == 3) {
                setIsFirstCardType32(1);
            } else {
                setIsFirstCardType32(0);
            }
        }
        res.setIsClearDesk(0);


        if (cardType <= 0) {
            cardType = CardUtils.cardResult2ReturnType(cardResult);
        }

        res.setCardType(cardType);
        res.setUserId(player.getUserId() + "");
        res.setSeat(player.getSeat());
        res.setIsPlay(2);
        if (player.getHandPais().size() == 1) {
            res.setIsBt(1);
        }

        boolean let = false;
        boolean isOver = player.getHandPais().size() == 0;

        Map<Long, Integer> stateMap = new HashMap<>();
        int passNum = 0;
        for (PdkPlayer pdkPlayer : seatMap.values()) {
            PlayCardRes.Builder copy = res.clone();
            if (pdkPlayer.getUserId() == player.getUserId()) {
                pdkPlayer.writeSocket(copy.build());
                continue;
            }

            if (isOver) {
                // ???????????????????????????????????????????????????????????????
                copy.setIsLet(0);

            }else {
                if(cardResult.getType() == 1000){
                    // 3???A?????????????????????
                    stateMap.put(pdkPlayer.getUserId(), 0);
                    passNum++;
                }else{
                    List<Integer> canPlayList = CardTypeTool.canPlay(pdkPlayer.getHandPais(), nowDisCardIds, false, pdkPlayer, this);
                    if (canPlayList.size() > 0) {
                        if(canPlayList.size()==pdkPlayer.getHandPais().size()){
                        	if(nowDisCardIds.size()==canPlayList.size()||card3Eq==0){
                        		pdkPlayer.setAutoFinalCards(canPlayList);
                        	}
                        }
                        CardUtils.Result result = CardUtils.calcCardValue(CardUtils.loadCards(canPlayList), siDai,isFirstCardType32==1,AAAZha==1);
                        if ((result.getType() == 3 || result.getType() == 33) && canPlayList.size() % 5 != 0) {
                            if (canPlayList.size() == pdkPlayer.getHandPais().size() && card3Eq != 1) {
                                copy.setIsLet(1);
                                let = true;
                                stateMap.put(pdkPlayer.getUserId(), 1);
                            } else {
                                stateMap.put(pdkPlayer.getUserId(), 0);
                            }
                        } else {
                            copy.setIsLet(1);
                            let = true;
                            // pdkPlayer.setIsNoLet(0);
                            stateMap.put(pdkPlayer.getUserId(), 1);
                        }
                    } else {
                        // ?????????????????????????????????
                        // pdkPlayer.setIsNoLet(1);
                        stateMap.put(pdkPlayer.getUserId(), 0);
                        passNum++;
                        pdkPlayer.getAutoFinalCards().clear();
                    }
                }
                if (passNum >= max_player_count - 1) {
                    setReplayDisCard();
                    for (PdkPlayer p : seatMap.values()) {
                        p.writeSocket(SendMsgUtil.buildComRes(WebSocketMsgType.req_code_pdk_playBack, getReplayDisCard()).build());
                    }
                }

            }
            pdkPlayer.writeSocket(copy.build());
        }

        if (cardResult.getType() == 100 || cardResult.getType() == 1000) {
            if (!isGoldRoom() || !let) {
                player.changeBoomCount(1);
            }

            if (!let) {
                // ??????????????? ???????????????
                if (isMatchRoom()) {
                    for (PdkPlayer pdkPlayer : seatMap.values()) {
                        if (pdkPlayer.getUserId() == player.getUserId()) {
                            if (isTwoPlayer()) {
                                pdkPlayer.changePlayBoomPoint(10);
                            } else {
                                pdkPlayer.changePlayBoomPoint(20);
                            }
                        } else {
                            pdkPlayer.changePlayBoomPoint(-10);
                        }
                    }
                } else {
                    for (PdkPlayer pdkPlayer : seatMap.values()) {
                        if (pdkPlayer.getUserId() == player.getUserId()) {
                            if (isTwoPlayer()) {
                                pdkPlayer.changePlayPoint(10);
                                pdkPlayer.changePlayBoomPoint(10);
                            } else {
                                pdkPlayer.changePlayPoint(20);
                                pdkPlayer.changePlayBoomPoint(20);
                            }
                        } else {
                            pdkPlayer.changePlayPoint(-10);
                            pdkPlayer.changePlayBoomPoint(-10);
                        }
                    }
                }
            }
        }
        if(!let){
            setNewRound(true);
            setIsFirstCardType32(0);
            CardUtils.Result result = CardUtils.calcCardValue(CardUtils.loadCards(player.getHandPais()), siDai,isFirstCardType32==1,AAAZha==1);
            Map<Integer, Integer> boom = CardUtils.findBoom(CardUtils.loadCards(player.getHandPais()), AAAZha == 1);
            boolean lastCards=true;
            if(result.getType()==0)
                lastCards=false;
            if(boom.size()>1)
                lastCards=false;
            if(boom.size()==1){
                for (Integer count:boom.values()) {
                    if(count!=player.getHandPais().size())
                        lastCards=false;
                }
            }
            if(result.getType()==11&&result.getMax()==15)
                lastCards=false;
            if(lastCards)
                player.setAutoFinalCards(new ArrayList<>(player.getHandPais()));
        }else{
            setNewRound(false);
        }
        if (isOver) {
            state = table_state.over;
        } else {
            int nextSeat = calcNextSeat(player.getSeat());
            PdkPlayer nextPlayer = seatMap.get(nextSeat);
            while (nextSeat != player.getSeat() && nextPlayer.getHandPais().size() == 0) {
                nextSeat = calcNextSeat(nextPlayer.getSeat());
                nextPlayer = seatMap.get(nextSeat);
            }

            Integer state = stateMap.remove(nextPlayer.getUserId());
            while (state != null && state.intValue() == 0) {

                if (nextPlayer.getUserId() != player.getUserId()) {
                    playCommand(nextPlayer, null);
                }

                nextSeat = calcNextSeat(nextPlayer.getSeat());
                nextPlayer = seatMap.get(nextSeat);
                if (nextPlayer == null) {
                    break;
                }
                while (nextSeat != player.getSeat() && nextPlayer.getHandPais().size() == 0) {
                    nextSeat = calcNextSeat(nextPlayer.getSeat());
                    nextPlayer = seatMap.get(nextSeat);
                }

                state = stateMap.remove(nextPlayer.getUserId());
            }
        }

    }

    public void setReplayDisCard(){
        List<PlayerCard> cards = new ArrayList<>();
        int size = noPassDisCard.size();
        for (int i = 0; i < 3&&i<size; i++) {
            cards.add(noPassDisCard.get(size-1-i));
        }
        setReplayDisCard(cards.toString());
        noPassDisCard.clear();
    }

    /**
     * ????????????????????????
     */
    public void clearIsNotLet() {
        for (PdkPlayer player : seatMap.values()) {
            player.setIsNoLet(0);
        }
    }

    /**
     * ??????
     *
     * @param player
     * @param cards
     */
    public void playCommand(PdkPlayer player, List<Integer> cards) {
        synchronized (this) {
            if (state != table_state.play) {
                return;
            }

            addPlayLog(player.getSeat(), cards, ",");
            StringBuilder sb = new StringBuilder("Pdk");
            sb.append("|").append(getId());
            sb.append("|").append(getPlayBureau());
            sb.append("|").append(player.getUserId());
            sb.append("|").append(player.getSeat());
            sb.append("|").append(player.isAutoPlay() ? 1 : 0);
            sb.append("|").append("chuPai");
            sb.append("|").append(cards);
            LogUtil.msgLog.info(sb.toString());
            if (cards != null && cards.size() > 0) {
                changeDisCardRound(1);
                // ?????????
                disCards(player, cards);
            } else {
                if (disCardRound > 0) {
                    changeDisCardRound(1);
                }
                notLet(player);
            }

            setLastActionTime(TimeUtil.currentTimeMillis());

            if (isOver()) {
                calcOver();
            } else {
                int nextSeat = calcNextSeat(player.getSeat());
                PdkPlayer nextPlayer = seatMap.get(nextSeat);
                if (!nextPlayer.isRobot()) {
                    nextPlayer.setNextAutoDisCardTime(TimeUtil.currentTimeMillis() + autoTimeOut);
                }
            }
        }
    }

    public int getAutoTimeOut() {
        return autoTimeOut;
    }

    /**
     * ??????????????????????????????
     *
     * @return 0 ???????????? 1???????????? 2????????????
     */
    public int isCanPlay() {
        if (seatMap.size() < getMaxPlayerCount()) {
            return 1;
        }
        for (PdkPlayer player : seatMap.values()) {
            if (player.getIsEntryTable() != SharedConstants.table_online) {
                // ?????????????????????
                broadIsOnlineMsg(player, player.getIsEntryTable());
                return 2;
            }
        }
        return 0;
    }

    @Override
    public <T> T getPlayer(long id, Class<T> cl) {
        return (T) playerMap.get(id);
    }

    @Override
    protected boolean quitPlayer1(Player player) {
        return false;
    }

    @Override
    protected boolean joinPlayer1(Player player) {
        return false;
    }

    @Override
    protected void initNext1() {
        setNowDisCardIds(null);
        setIsFirstCardType32(0);
        replayDisCard="";
        timeNum = 0;
        newRound = true;
        finishFapai=0;
        isSendPiaoFenMsg=0;
    }

    @Override
    public int getPlayerCount() {
        return playerMap.size();
    }

    @Override
    protected void sendDealMsg() {
        sendDealMsg(0);
    }

    @Override
    protected void sendDealMsg(long userId) {
        int nextSeat = getNextDisCardSeat();
        PdkPlayer nextPlayer = seatMap.get(nextSeat);
        int timeout;
        if (!nextPlayer.isRobot()) {
            if (matchId > 0L && disCardRound == 0) {
                timeout = autoTimeOut + 5000;
            } else if(autoPlay && disCardRound == 0){
                timeout = autoTimeOut + 5000;
            }else{
                timeout = autoTimeOut;
            }
            nextPlayer.setNextAutoDisCardTime(TimeUtil.currentTimeMillis() + timeout);
        } else {
            timeout = autoTimeOut;
        }

        for (PdkPlayer tablePlayer : seatMap.values()) {
            DealInfoRes.Builder res = DealInfoRes.newBuilder();
            res.addAllHandCardIds(tablePlayer.getHandPais());
            res.setNextSeat(nextSeat);
            res.setGameType(getWanFa());// 1????????? 2??????
            res.setBanker(lastWinSeat);

            res.addXiaohu(nextPlayer.isAutoPlay() ? 1 : 0);
            res.addXiaohu(timeout);

            tablePlayer.writeSocket(res.build());
            
            if(tablePlayer.isAutoPlay()) {
       		 addPlayLog(tablePlayer.getSeat(), PdkConstants.action_tuoguan + "",1 + "");
            }
            if(tablePlayer.getSeat()==nextSeat&&playType == GameUtil.play_type_15){
                CardUtils.Result result = CardUtils.calcCardValue(CardUtils.loadCards(tablePlayer.getHandPais()), siDai,isFirstCardType32==1,AAAZha==1);
                Map<Integer, Integer> boom = CardUtils.findBoom(CardUtils.loadCards(tablePlayer.getHandPais()), AAAZha == 1);
                boolean lastCards=true;
                if(result.getType()==0)
                    lastCards=false;
                if(boom.size()>1)
                    lastCards=false;
                if(boom.size()==1){
                    for (Integer count:boom.values()) {
                        if(count!=tablePlayer.getHandPais().size())
                            lastCards=false;
                    }
                }
                if(lastCards)
                    tablePlayer.setAutoFinalCards(new ArrayList<>(tablePlayer.getHandPais()));
            }
        }
    }

    @Override
    protected void robotDealAction() {
    }

    @Override
    protected void deal() {

    }

    @Override
    public Map<Long, Player> getPlayerMap() {
        Object o = playerMap;
        return (Map<Long, Player>) o;
    }

    @Override
    public int getMaxPlayerCount() {
        return max_player_count;
    }


    public void setMaxPlayerCount(int maxPlayerCount) {
        this.max_player_count = maxPlayerCount;
        changeExtend();
    }

    public List<Integer> getNowDisCardIds() {
        return nowDisCardIds;
    }

    public void setNowDisCardIds(List<Integer> nowDisCardIds) {
        if (nowDisCardIds == null) {
            this.nowDisCardIds.clear();

        } else {
            this.nowDisCardIds = nowDisCardIds;

        }
        dbParamMap.put("nowDisCardIds", JSON_TAG);
    }

    public boolean saveSimpleTable() throws Exception {
        TableInf info = new TableInf();
        info.setMasterId(masterId);
        info.setRoomId(0);
        info.setPlayType(playType);
        info.setTableId(id);
        info.setTotalBureau(totalBureau);
        info.setPlayBureau(1);
        info.setServerId(GameServerConfig.SERVER_ID);
        info.setCreateTime(new Date());
        info.setDaikaiTableId(daikaiTableId);

        changeExtend();

        info.setExtend(buildExtend());
        TableDao.getInstance().save(info);
        loadFromDB(info);
        return true;
    }

    public boolean createSimpleTable(Player player, int play, int bureauCount, List<Integer> params, List<String> strParams, boolean saveDb) throws Exception {
        return createTable(new CreateTableInfo(player, TABLE_TYPE_NORMAL, play, bureauCount, params, saveDb));
    }

    public void createTable(Player player, int play, int bureauCount, List<Integer> params) throws Exception {
        createTable(new CreateTableInfo(player, TABLE_TYPE_NORMAL, play, bureauCount, params, true));
    }
    public boolean createTable(Player player, int play, int bureauCount, List<Integer> params, boolean saveDb) throws Exception {
        return createTable(new CreateTableInfo(player,TABLE_TYPE_NORMAL,play,bureauCount,params,saveDb));
    }

    @Override
    public boolean createTable(CreateTableInfo createTableInfo) throws Exception {
        Player player = createTableInfo.getPlayer();
        int play = createTableInfo.getPlayType();
        int bureauCount =createTableInfo.getBureauCount();
        int tableType = createTableInfo.getTableType();
        List<Integer> params = createTableInfo.getIntParams();
        List<String> strParams = createTableInfo.getStrParams();
        boolean saveDb = createTableInfo.isSaveDb();

        //objects??????????????????  [??????,?????????15??????16??????,this.niao,this.leixing,this.zhuang,this.niaoPoint,????????????3,??????,??????????????????
        long id = getCreateTableId(player.getUserId(), play);
        if (id <= 0) {
            return false;
        }
        if (saveDb) {
            TableInf info = new TableInf();
            info.setMasterId(player.getUserId());
            info.setTableType(tableType);
            info.setRoomId(0);
            info.setPlayType(play);
            info.setTableId(id);
            info.setTotalBureau(bureauCount);
            info.setPlayBureau(1);
            info.setServerId(GameServerConfig.SERVER_ID);
            info.setCreateTime(new Date());
            info.setDaikaiTableId(daikaiTableId);
            info.setExtend(buildExtend());
            TableDao.getInstance().save(info);
            loadFromDB(info);
        } else {
            setPlayType(play);
            setDaikaiTableId(daikaiTableId);
            this.id = id;
            this.totalBureau = bureauCount;
            this.playBureau = 1;
        }

        setLastActionTime(TimeUtil.currentTimeMillis());
        if (params.size() >= 7) {
            setIsFirstRoundDisThree(params.get(6).intValue());
        }
        // ??????????????????
        if (params.size() >= 8) {
            int isTwoPlayer = params.get(7).intValue();
            setMaxPlayerCount(isTwoPlayer);
        }
        if (params.size() >= 9) {
            setShowCardNumber(params.get(8).intValue());
        }
        if (params.size() >= 10) {
            setPayType(params.get(9).intValue());
        }
        if (params.size() >= 11) {
            setRedTen(params.get(10).intValue());
        }
        int siDai = StringUtil.getIntValue(params, 11, 0);
        if (siDai == 3 || siDai == 2) {
            setSiDai(siDai);
        } else {
            setSiDai(0);
        }
//        if (isTwoPlayer()) {
//            setIsFirstRoundDisThree(0);
//        }

        card3Eq = StringUtil.getIntValue(params, 12, card3Eq);

        this.autoPlay = StringUtil.getIntValue(params, 21, 0) >= 1;
        int time =StringUtil.getIntValue(params, 21, 0);
        if(time==1) {
            time =60;
        }

        this.jiaBei = StringUtil.getIntValue(params, 22, 0);
        this.jiaBeiFen = StringUtil.getIntValue(params, 23, 100);
        this.jiaBeiShu = StringUtil.getIntValue(params, 24, 1);
        if(this.getMaxPlayerCount() != 2){
            jiaBei = 0 ;
        }
        this.isNoBoom=StringUtil.getIntValue(params, 25, 0);
        this.isPlayBack=StringUtil.getIntValue(params, 26, 0);
        autoPlayGlob = StringUtil.getIntValue(params, 27, 0);
        this.daNiaoFen = StringUtil.getIntValue(params, 28, 0);
        this.chai4Zha = StringUtil.getIntValue(params, 29, 1);
        this.AAAZha = StringUtil.getIntValue(params, 30, 0);
        if(daNiaoFen==0)
            this.piaoFenType = StringUtil.getIntValue(params, 31, 0);
        if(max_player_count==2){
            int belowAdd = StringUtil.getIntValue(params, 32, 0);
            if(belowAdd<=100&&belowAdd>=0)
                this.belowAdd=belowAdd;

            int below = StringUtil.getIntValue(params, 33, 0);
            if(below<=100&&below>=0){
                this.below=below;
                if(belowAdd>0&&below==0)
                    this.below=10;
            }
        }

        if(autoPlay) {
            autoTimeOut =autoTimeOut2 =time*1000;
        }
        changeExtend();
        return true;
    }

    @Override
    protected void initNowAction(String nowAction) {

    }

    @Override
    public void initExtend0(JsonWrapper wrapper) {
//		JsonWrapper wrapper = new JsonWrapper(info);
        isFirstRoundDisThree = wrapper.getInt(1, 0);
        max_player_count = wrapper.getInt(2, 3);
        if (max_player_count == 0) {
            max_player_count = 3;
        }
        showCardNumber = wrapper.getInt(3, 0);
        redTen = wrapper.getInt(4, 0);
        siDai = wrapper.getInt(5, 0);
        isFirstCardType32 = wrapper.getInt(6, 0);
        if (payType == -1) {
            String isAAStr = wrapper.getString("isAAConsume");
            if (!StringUtils.isBlank(isAAStr)) {
                this.payType = Boolean.parseBoolean(wrapper.getString("isAAConsume")) ? 1 : 2;
            } else {
                payType = 1;
            }
        }

        card3Eq = wrapper.getInt("card3Eq", card3Eq);

        jiaBei = wrapper.getInt(7, 0);
        jiaBeiFen = wrapper.getInt(8, 0);
        jiaBeiShu = wrapper.getInt(9, 0);
        isNoBoom = wrapper.getInt(10,0);
        isPlayBack = wrapper.getInt(11,0);
        replayDisCard = wrapper.getString(12);
        autoTimeOut = wrapper.getInt(13,0);
        autoPlayGlob = wrapper.getInt(14,0);
        daNiaoFen = wrapper.getInt(15,0);
        chai4Zha = wrapper.getInt(16,1);
        newRound = wrapper.getInt(17, 1) == 1;
        AAAZha = wrapper.getInt(18, 1);
        piaoFenType=wrapper.getInt(19, 0);
        isSendPiaoFenMsg=wrapper.getInt(20, 0);
        finishFapai=wrapper.getInt(21, 0);
        belowAdd=wrapper.getInt(22, 0);
        below=wrapper.getInt(23, 0);
        if(belowAdd>0&&below==0)
            below=10;
        autoTimeOut2 = autoTimeOut;
        //???????????????
        if(autoPlay && autoTimeOut<=1) {
            autoTimeOut2 = autoTimeOut=60000;
        }

        //TODO ??????????????????????????????????????????????????????,??????????????????????????????????????????????????????
        // -----------------start------------------------------
        if (finishFapai == 0) {
            // ?????????????????????????????????
            for (PdkPlayer player : seatMap.values()) {
                if (player.getHandPais() != null && player.getHandPais().size() > 0) {
                    finishFapai = 1;
                    break;
                }
            }
        }
        // -----------------end------------------------------

    }

    @Override
    protected String buildNowAction() {
        return null;
    }

    @Override
    public void setConfig(int index, int val) {

    }

    /**
     * ????????????msg
     *
     * @param over      ??????????????????
     * @param winPlayer ????????????
     * @return
     */
    public ClosingInfoRes.Builder sendAccountsMsg(boolean over, Player winPlayer, boolean isBreak) {
        List<ClosingPlayerInfoRes> list = new ArrayList<>();
        List<ClosingPlayerInfoRes.Builder> builderList = new ArrayList<>();

        int minPointSeat = 0;
        int minPoint = 0;
        if (winPlayer != null) {
            for (PdkPlayer player : seatMap.values()) {
                if (player.getUserId() == winPlayer.getUserId()) {
                    continue;
                }
                if (minPoint == 0 || player.getPoint() < minPoint) {
                    minPoint = player.getPlayPoint();
                    minPointSeat = player.getSeat();
                }
            }
        }

        for (PdkPlayer player : seatMap.values()) {
            ClosingPlayerInfoRes.Builder build = null;
            if (over) {
                build = player.bulidTotalClosingPlayerInfoRes(this);
            } else {
                build = player.bulidOneClosingPlayerInfoRes(this);

            }
            //?????????????????????????????????
            //?????????
            //????????????
            List<Integer> allCard = new ArrayList<Integer>();
            for (Integer v : player.getHandPais()) {
                if (!allCard.contains(v)) {
                    allCard.add(v);
                }
            }
            //??????????????????
            for (List<Integer> c : player.getOutPais()) {
                for (Integer v : c) {
                    if (!allCard.contains(v)) {
                        allCard.add(v);
                    }
                }
            }

            JSONArray jsonArray = new JSONArray();
            for (int card : allCard) {
                if (card != 0) {
                    jsonArray.add(card);
                }
            }
            build.addExt(jsonArray.toString()); //0
            //?????????
            build.addExt(player.getPlayBoomPoint() + "");//1
            build.addExt(player.getRedTenPai() + "");//2

            if (isGoldRoom()) {
                build.setTotalPoint((int)player.getWinGold());
                build.setPoint((int)player.getWinGold());

                build.addExt("1");//3
                build.addExt(player.loadAllGolds() <= 0 ? "1" : "0");//4
                build.addExt(String.valueOf(player.getWinGold()));//5
            } else {
                build.addExt("0");//3
                build.addExt("0");//4
                build.addExt("0");//5
            }

            build.addExt(String.valueOf(player.getCurrentLs()));//6
            build.addExt(String.valueOf(player.getMaxLs()));//7
            build.addExt(String.valueOf(matchId));//8
            if(isGoldRoom()) {
                build.addExt(String.valueOf(getGoldRoom().getTicket()));//9
            }else{
                build.addExt("0");//9
            }

            if (winPlayer != null) {
                if (player.getSeat() == minPointSeat) {
                    build.setIsHu(1);
                    player.changeCutCard(1);
                } else {
                    build.setIsHu(0);
                    player.changeCutCard(0);
                }
            }

            if (winPlayer != null && player.getUserId() == winPlayer.getUserId()) {
                // ?????????????????????????????????????????????
                builderList.add(0, build);
            } else {
                builderList.add(build);
            }

            //?????????
            if(isCreditTable()){
                player.setWinLoseCredit(player.getTotalPoint() * creditDifen);
            }

        }

        //???????????????
        if (isCreditTable()) {
            //??????????????????
            calcNegativeCredit();

            long dyjCredit = 0;
            for (PdkPlayer player : seatMap.values()) {
                if (player.getWinLoseCredit() > dyjCredit) {
                    dyjCredit = player.getWinLoseCredit();
                }
            }
            for (ClosingPlayerInfoRes.Builder builder : builderList) {
                PdkPlayer player = seatMap.get(builder.getSeat());
                calcCommissionCredit(player, dyjCredit);

                builder.addExt(player.getWinLoseCredit() + "");      //10
                builder.addExt(player.getCommissionCredit() + "");   //11
                builder.addExt(player.getNiaoFen()+"");// 12

                // 2019-02-26??????
                builder.setWinLoseCredit(player.getWinLoseCredit());
                builder.setCommissionCredit(player.getCommissionCredit());
            }
        } else if (isGroupTableGoldRoom()) {
            // -----------??????????????????---------------------------------
            for (PdkPlayer player : seatMap.values()) {
                player.setWinGold(player.getTotalPoint() * gtgDifen);
            }
            calcGroupTableGoldRoomWinLimit();
            for (ClosingPlayerInfoRes.Builder builder : builderList) {
                PdkPlayer player = seatMap.get(builder.getSeat());
                builder.setWinLoseCredit(player.getWinGold());
            }
        } else {
            for (ClosingPlayerInfoRes.Builder builder : builderList) {
                PdkPlayer player = seatMap.get(builder.getSeat());
                builder.addExt(0 + ""); //10
                builder.addExt(0 + ""); //11
                builder.addExt(player.getNiaoFen()+"");// 12
            }
        }
        for (ClosingPlayerInfoRes.Builder builder : builderList) {
            PdkPlayer player = seatMap.get(builder.getSeat());
            builder.addExt(player.getPiaoFen() + ""); //13
            //??????
//            builder.addExt(player.getXiPaiCount() +"");//14
//            builder.addExt(player.getXiPaiConsume() +"");//15

            list.add(builder.build());
        }

        ClosingInfoRes.Builder res = ClosingInfoRes.newBuilder();
        res.setIsBreak(isBreak ? 1 : 0);
        res.setWanfa(getWanFa());
        res.addAllClosingPlayers(list);
        res.addAllExt(buildAccountsExt(over?1:0));
        if (isTwoPlayer()) {
            res.addAllCutCard(this.cutCardList);
        }
        if (over && isGroupRoom() && !isCreditTable()) {
            res.setGroupLogId((int) saveUserGroupPlaylog());
        }
        for (PdkPlayer player : seatMap.values()) {
            player.writeSocket(res.build());
        }

        return res;
    }

    public List<String> buildAccountsExt(int over) {
        List<String> ext = new ArrayList<>();
        ext.add(id + "");//0
        ext.add(masterId + "");//1
        ext.add(TimeUtil.formatTime(TimeUtil.now()));//2
        ext.add(playType + "");//3
        //?????????????????????
        ext.add(playBureau + "");//4
        ext.add(isGroupRoom() ? "1" : "0");//5
        //???????????????0
        ext.add(CommonUtil.isPureNumber(modeId) ? modeId : "0");//6
        int ratio;
        int pay;
        if (isMatchRoom()) {
            ratio = (int) matchRatio;
            pay = 0;
        } else if (isGoldRoom()) {
            ratio = 0;
            pay = 0;
        }  else if (isCompetitionRoom()) {
            ratio = GameConfigUtil.loadGoldRatio(modeId);
            pay = PayConfigUtil.get(playType, totalBureau, max_player_count, payType == 1 ? 0 : 1, modeId);
        } else {
            ratio = 1;
            pay = loadPayConfig(payType);
        }
        ext.add(String.valueOf(ratio));//7
        ext.add(String.valueOf(pay >= 0 ? pay : 0));//8
        ext.add(String.valueOf(payType));//9
        ext.add(String.valueOf(playedBureau));//10

        ext.add(String.valueOf(matchId));//11
        ext.add(isGroupRoom() ? loadGroupId() : "");//12

        ext.add(creditMode + ""); //13
        ext.add(creditJoinLimit + "");//14
        ext.add(creditDissLimit + "");//15
        ext.add(creditDifen + "");//16
        ext.add(creditCommission + "");//17
        ext.add(creditCommissionMode1 + "");//18
        ext.add(creditCommissionMode2 + "");//19
        ext.add((autoPlay ? 1 : 0) + "");//20
        ext.add(jiaBei + "");//21
        ext.add(jiaBeiFen + "");//22
        ext.add(jiaBeiShu + "");//23
        ext.add(over+""); // 24
        ext.add(daNiaoFen+""); // 25
        ext.add(chai4Zha+""); // 25
        return ext;
    }

    @Override
    public String loadGameCode() {
        return GAME_CODE;
    }

    @Override
    public void sendAccountsMsg() {
        calcPointBeforeOver();
        ClosingInfoRes.Builder builder = sendAccountsMsg(true, null, true);
        saveLog(true, 0l, builder.build());
    }

    @Override
    public Class<? extends Player> getPlayerClass() {
        return PdkPlayer.class;
    }

    @Override
    public int getWanFa() {
        return SharedConstants.game_type_pdk;
    }

    @Override
    public void checkReconnect(Player player) {
        PdkTable table = player.getPlayingTable(PdkTable.class);
        player.writeSocket(SendMsgUtil.buildComRes(WebSocketMsgType.req_code_pdk_playBack, table.getReplayDisCard()).build());
        sendPiaoReconnect(player);
    }

    private void sendPiaoReconnect(Player player){
        if(piaoFenType==0||max_player_count!=getPlayerCount())
            return;
        int count=0;
        for(Map.Entry<Integer,PdkPlayer> entry:seatMap.entrySet()){
            player_state state = entry.getValue().getState();
            if(state==player_state.play||state==player_state.ready)
                count++;
        }
        if(count!=max_player_count)
            return;

        for(Map.Entry<Integer,PdkPlayer> entry:seatMap.entrySet()){
            PdkPlayer p = entry.getValue();
            if(p.getUserId()==player.getUserId()){
                if(!p.isAlreadyPiaoFen()){
                    player.writeComMessage(WebSocketMsgType.res_code_pdk_piaofen,piaoFenType);
                    continue;
                }
            }
        }
        for(Map.Entry<Integer,PdkPlayer> entry:seatMap.entrySet()){
            PdkPlayer p = entry.getValue();
            if(p.getUserId()!=player.getUserId()){
                List<Integer> l=new ArrayList<>();
                l.add((int)p.getUserId());
                l.add(p.getPiaoFen());
                player.writeComMessage(WebSocketMsgType.res_code_pdk_broadcast_piaofen, l);
            }
        }

    }

    public void setWinLostPiaoFen(PdkPlayer win,PdkPlayer lost) {
        lost.setWinLostPiaoFen(-win.getPiaoFen()-lost.getPiaoFen());
        win.setWinLostPiaoFen(win.getWinLostPiaoFen()+win.getPiaoFen()+lost.getPiaoFen());
    }

    // ?????????????????????
    public boolean isTwoPlayer() {
        return max_player_count == 2;
    }

    // ??????????????????????????????
    public boolean isShowCardNumber() {
        return 1 == getShowCardNumber();
    }

    public void checkCompetitionPlay() {
        synchronized (this) {
            checkCanFinalDis();
            checkAutoPlay();
        }
    }

    public void checkCanFinalDis() {
        PdkPlayer player = seatMap.get(getNextDisCardSeat());
        if(player!=null&&player.getAutoFinalCards().size()>0)
            playCommand(player,player.getAutoFinalCards());
    }

    @Override
    public void checkAutoPlay() {
        synchronized (this) {
            if(!autoPlay){
                return;
            }
            // ??????????????????????????????
            if (getSendDissTime() > 0) {
                for (PdkPlayer player : seatMap.values()) {
                    if (player.getLastCheckTime() > 0) {
                        player.setLastCheckTime(player.getLastCheckTime() + 1 * 1000);
                    }
                }
                return;
            }

            if (isAutoPlayOff()) {
                // ????????????
                for (int seat : seatMap.keySet()) {
                    PdkPlayer player = seatMap.get(seat);
                    player.setAutoPlay(false, this);
                    player.setLastOperateTime(System.currentTimeMillis());
                }
                return;
            }

            // ????????????
            if (state == table_state.ready && playedBureau > 0 ) {
                ++timeNum;
                for (PdkPlayer player : seatMap.values()) {
                    // ????????????????????????5???????????????
                    if (timeNum >= 5 && player.isAutoPlay()) {
                        autoReady(player);
                    } else if (timeNum >= 30) {
                        autoReady(player);
                    }
                }
                return;
            }
            // ????????????
            if (tableStatus == PdkConstants.TABLE_STATUS_DANIAO) {
                for (PdkPlayer player : seatMap.values()) {
                    if(player.getNiaoFen() != -1){
                        continue;
                    }
                    boolean auto = checkPlayerAuto(player, autoTimeOut);
                    if (auto) {
                        daNiao(player, 0);
                    }
                }
                return;
            }else if (daNiaoFen==0&&piaoFenType>0&&isSendPiaoFenMsg == 1 && finishFapai == 0) {
                //???????????????????????????
                for (PdkPlayer player : seatMap.values()) {
                    if(player.isAlreadyPiaoFen()){
                        continue;
                    }
                    boolean auto = checkPlayerAuto(player, autoTimeOut);
                    if (auto) {
                        piaoFen(player, piaoFenType>=3?0:piaoFenType);
                    }
                }
                return;
            }

            if(state != table_state.play){
                return;
            }

            int timeout;
            PdkPlayer player = seatMap.get(getNextDisCardSeat());
            if (player == null) {
                return;
            } else if(autoPlay){
                timeout = autoTimeOut;
                if (disCardRound == 0) {
                    timeout = autoTimeOut ;
                }
            }else{
                return;
            }
            long autoPlayTime = ResourcesConfigsUtil.loadIntegerValue("ServerConfig", "autoPlayTimePdk", 2 * 1000);
            long now = TimeUtil.currentTimeMillis();
            boolean auto = player.isAutoPlay();
            if (!auto) {
                if (GameServerConfig.isAbroad()) {
                    if (!player.isRobot() && now >= player.getNextAutoDisCardTime()) {
                        auto = true;
                        player.setAutoPlay(true, this);
                    }
                } else {
                    auto = checkPlayerAuto(player,timeout);
                }
            }

            if (auto) {
                boolean autoPlay = false;
                if (GameServerConfig.isAbroad()) {
                    autoPlay = true;
                }
                if (player.getAutoPlayTime() == 0L && !autoPlay) {
                    player.setAutoPlayTime(now);
                } else if (autoPlay || (player.getAutoPlayTime() > 0L && now - player.getAutoPlayTime() >= autoPlayTime)) {
                    player.setAutoPlayTime(0L);

                    if (state == table_state.play) {
                        // ????????????3??????????????????3????????????3??????????????????????????????????????????
                        if(checkHeiTao3ChuTou(player)){
                            return;
                        }
                        List<Integer> curList = new ArrayList<>(player.getHandPais());
                        if (curList.isEmpty()) {
                            return;
                        }
                        List<Integer> oppo;
                        if (disCardSeat != player.getSeat()) {
                            oppo = getNowDisCardIds();
                        } else {
                            oppo = null;
                        }

                        boolean nextDan = seatMap.get(calcNextSeat(player.getSeat())).getHandPais().size() == 1;
                        oppo = oppo == null ? null : new ArrayList<>(oppo);
                        List<Integer> list = null;
                        if(player.isRobot()){
//                        	List<Integer> residueList = CardTypeTool.getAllResidueCards(curList, getAllPdkCards(), getAllPlayOutCard());
//                        	list = robotChupai(curList, oppo,residueList,nextDan);
//                        	if(list == null || list.size() <= 0){
                        		list = CardTypeTool.getBestAI2(curList, oppo, nextDan, this);
//                        	}
                        }else{
                        	list = CardTypeTool.getBestAI2(curList, oppo, nextDan, this);
                        }

                        if (oppo != null && oppo.size() > 0 && oppo.size() % 5 == 0 && list != null && list.size() % 5 != 0 && card3Eq == 1) {
                            CardUtils.Result result = CardUtils.calcCardValue(CardUtils.loadCards(list), siDai,isFirstCardType32==1,AAAZha==1);
                            if (result.getType() == 3 || result.getType() == 33) {
                                list = null;
                            }
                        }

                        playCommand(player,list);
                    }
                }
            }
        }
    }


    public List<Integer> getRobotCards(List<List<Integer>> testPai,boolean isbaodan){
    	Map<Integer, List<List<Integer>>> map = new HashMap<>();

    	List<List<Integer>> list = new ArrayList<>();
    	list.add(testPai.size() > 1 ?testPai.get(1):new ArrayList<>());
    	map.put(1, list);
    	List<Integer> residueList = CardTypeTool.getAllResidueCards(testPai.get(0), getAllPdkCards(), map);
    	return robotChupai(testPai.get(0), testPai.size() > 2 ?testPai.get(2):new ArrayList<>(), residueList, isbaodan);
    }

    public static List<Integer> robotChupai(List<Integer> handCards,List<Integer> disCards,List<Integer> residueList,boolean nextDan){
		/* First of all, we create the BT library. */
		IBTLibrary btLibrary = new PaodekuaiBTLibrary();
		/* Then we create the initial context that the tree will use. */
		IContext context = ContextFactory.createContext(btLibrary);
		/*
		 * Now we are assuming that the marine that is going to be *
		 * controlled has an id of "terranMarine1"
		 */
		context.setVariable("CurrentEntityID", "terranMarine1");

		/* Now we get the Model BT to run. */
		ModelTask terranMarineTree = btLibrary.getBT("pdk");

		/* Then we create the BT Executor to run the tree. */
		IBTExecutor btExecutor = BTExecutorFactory.createBTExecutor(terranMarineTree, context);
		System.out.println("??????"+handCards);
		System.out.println("???????????????"+disCards);
		context.setVariable("handCards", new ArrayList<>(handCards));
		context.setVariable("initHandCards", new ArrayList<>(handCards));
		context.setVariable("disCards", disCards);
		context.setVariable("residueCards", residueList);
		context.setVariable("nextDan", nextDan?1:0);
		int step = 1;
		/* And finally we run the tree through the BT Executor. */
		do {
			System.out.println("--------------Step = " + step);
			btExecutor.tick();
//			System.out.println("btExecutor??????????"+btExecutor.getStatus());
//			System.out.println("??????????????????????"+giveCards);
//			System.out.println("??????????????????"+handCards);
//			Thread.sleep(1000);
			step++;
		} while (btExecutor.getStatus() == Status.RUNNING);

		System.out.println();
		if(context.getVariable("chupai") == null){
			System.out.println("?????????????????????,????????????");
			return null;
		}else{
			List<Integer> chupai = (List<Integer>) context.getVariable("chupai");
			System.out.println("??????:" + chupai);
			return chupai;
		}


    }

    /**
     * ????????????????????????3??????
     *
     * @param player
     * @return
     */
    public boolean checkHeiTao3ChuTou(PdkPlayer player) {
        if (getIsFirstRoundDisThree() != 1) {
            return false;
        }
        if (getPlayBureau() != 1 || getDisCardRound() != 0) {
            return false;
        }
        List<Integer> curList = new ArrayList<>(player.getHandPais());
        List<Integer> card3List = new ArrayList<>();
        boolean hasHeiTao3 = false;
        Iterator<Integer> iterator = curList.iterator();
        while (iterator.hasNext()) {
            int card = iterator.next();
            if (CardTool.loadCardValue(card) == 3||(getPlayType()== GameUtil.play_type_11&&CardTool.loadCardValue(card) == 6)) {
                if (card == 403||(getPlayType()== GameUtil.play_type_11&&card == 406)) {
                    hasHeiTao3 = true;
                    if (card3List.size() > 0) {
                        card3List.add(0, card);
                    } else {
                        card3List.add(card);
                    }
                } else {
                    card3List.add(card);
                }
                iterator.remove();
            }
        }
        if (!hasHeiTao3) {
            return false;
        }
        if (card3List.size() == 1 || card3List.size() == 2 || card3List.size() == 4) {
            // 1??????2??????4????????????
            playCommand(player, card3List);
        } else {
            // 3?????????????????????
            Map<Integer, Integer> map = CardTool.loadCards(curList);
            List<Integer> retList = new ArrayList<>();
            // ????????????,?????????1??????2??????3??????????????????????????????
            for (int i = 1; i <= 2; i++) {
                for (Map.Entry<Integer, Integer> kv : map.entrySet()) {
                    if (kv.getValue().intValue() == i) {
                        retList.addAll(CardTool.loadCards(curList, kv.getKey().intValue()));
                    }
                    if (retList.size() >= 2) {
                        break;
                    }
                }
                if (retList.size() >= 2) {
                    break;
                }
            }
            if (retList.size() >= 2) {
                card3List.addAll(retList.subList(0, 2));
                playCommand(player, card3List);
            } else {
                // ????????????????????????3
                playCommand(player, card3List.subList(0, 2));
            }
        }
        return true;
    }

    public boolean checkPlayerAuto(PdkPlayer player ,int timeout){
        if(player.isAutoPlay()){
            return true;
        }
        long now = TimeUtil.currentTimeMillis();
        boolean auto = false;
        if (player.isAutoPlayChecked() || (player.getAutoPlayCheckedTime() >= timeout && !player.isAutoPlayCheckedTimeAdded())) {
            player.setAutoPlayChecked(true);
            timeout = autoTimeOut2;
        }
        if (player.getLastCheckTime() > 0) {
            int checkedTime = (int) (now - player.getLastCheckTime());
            if (checkedTime >= timeout) {
                auto = true;
            }
            if(auto){
                player.setAutoPlay(true, this);
            }
        } else {
            player.setLastCheckTime(now);
            player.setAutoPlayCheckedTimeAdded(false);
        }

        return auto;
    }

    private String cardMarkerToJSON() {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<Integer, PdkPlayer> entry : seatMap.entrySet()) {
            jsonObject.put("" + entry.getKey(), entry.getValue().getOutPais());
        }
        return jsonObject.toString();
    }

    public int getIsFirstRoundDisThree() {
        return isFirstRoundDisThree;
    }

    public void setIsFirstRoundDisThree(int isFirstRoundDisThree) {
        this.isFirstRoundDisThree = isFirstRoundDisThree;
        changeExtend();
    }

    public int getShowCardNumber() {
        return showCardNumber;
    }

    public void setShowCardNumber(int showCardNumber) {
        this.showCardNumber = showCardNumber;
        changeExtend();
    }


    @Override
    public void createTable(Player player, int play, int bureauCount, List<Integer> params, List<String> strParams,Object... objects) throws Exception {
        createTable(player, play, bureauCount, params);
    }

    @Override
    public void createTable(Player player, int play, int bureauCount, Object... objects) throws Exception {

    }

    @Override
    public boolean isCreditTable(List<Integer> params){
        return params != null && params.size() > 13 && StringUtil.getIntValue(params, 13, 0) == 1;
    }

    public String getGameName(){
        return "?????????";
    }

    public static final List<Integer> wanfaList = Arrays.asList(15, 16,11);

    public static void loadWanfaTables(Class<? extends BaseTable> cls) {
        for (Integer integer : wanfaList) {
            TableManager.wanfaTableTypesPut(integer, cls);
        }
    }

    @Override
    public boolean allowRobotJoin() {
        return StringUtils.contains(ResourcesConfigsUtil.loadServerPropertyValue("robot_modes", ""), new StringBuilder().append("|").append(modeId).append("|").toString());
    }

    public void setTableStatus(int tableStatus) {
        this.tableStatus = tableStatus;
    }

    public int getTableStatus() {
        return tableStatus;
    }

    public int getDaNiaoFen() {
        return daNiaoFen;
    }

    public void setDaNiaoFen(int daNiaoFen) {
        this.daNiaoFen = daNiaoFen;
    }

    @Override
    public boolean isAllReady() {
        if(daNiaoFen==0&&piaoFenType>0){
            return isAllReady2();
        }else if(piaoFenType==0){
            return isAllReady1();
        }else {
            return super.isAllReady();
        }
    }

    public boolean isAllReady1() {
        if (super.isAllReady()) {
            if (playBureau != 1) {
                return true;
            }
            // ???????????????????????????????????????
            if (daNiaoFen > 0) {
                setTableStatus(PdkConstants.TABLE_STATUS_DANIAO);
                boolean isAllDaNiao = true;
                ComMsg.ComRes.Builder com = SendMsgUtil.buildComRes(WebSocketMsgType.res_code_pdk_daniao, 1);
                for (PdkPlayer player : seatMap.values()) {
                    if (player.getNiaoFen() < 0) {
                        // ???????????????
                        player.writeSocket(com.build());
                        isAllDaNiao = false;
                    }
                }
                if (!isAllDaNiao) {
                    broadMsgRoomPlayer(com.build());
                }
                return isAllDaNiao;
            } else {
                for (PdkPlayer player : seatMap.values()) {
                    player.setNiaoFen(0);
                }
                return true;
            }
        }
        return false;
    }

    public void daNiao(PdkPlayer player, int niaoFen) {
        if (this.getDaNiaoFen() > 0) {
            player.setNiaoFen(niaoFen);
            StringBuilder sb = new StringBuilder("Pdk");
            sb.append("|").append(getId());
            sb.append("|").append(getPlayBureau());
            sb.append("|").append(player.getUserId());
            sb.append("|").append(player.getSeat());
            sb.append("|").append(player.isAutoPlay() ? 1 : 0);
            sb.append("|").append("daNiao");
            sb.append("|").append(niaoFen);
            LogUtil.msgLog.info(sb.toString());
            ComMsg.ComRes.Builder com = SendMsgUtil.buildComRes(WebSocketMsgType.res_code_pdk_daniao, 2, player.getSeat(), player.getNiaoFen());
            this.broadMsgToAll(com.build());
            this.checkDeal();
        }
    }


    public boolean isAllReady2() {
        if (getPlayerCount() < getMaxPlayerCount()) {
            return false;
        }
        for (Player player : getSeatMap().values()) {
            if(!player.isRobot()){
                if(piaoFenType>1){
                    if (!(player.getState() == player_state.ready||player.getState() == player_state.play))
                        return false;
                }else {
                    if(player.getState() != player_state.ready)
                        return false;
                }
            }
        }
        if(finishFapai==1)
            return false;
        changeTableState(table_state.play);
        if (piaoFenType >=3 ) {
            boolean piaoFenOver = true;
            for (PdkPlayer player : playerMap.values()) {
                if(!player.isAlreadyPiaoFen()){
                    piaoFenOver = false;
                    break;
                }
            }
            if(!piaoFenOver){
                if (isSendPiaoFenMsg==0 && finishFapai==0) {
                    LogUtil.msgLog.info("pdk|sendPiaoFen|" + getId() + "|" + getPlayBureau());
                    ComRes msg = SendMsgUtil.buildComRes(WebSocketMsgType.res_code_pdk_piaofen, piaoFenType).build();
                    for (PdkPlayer player : playerMap.values()) {
                        if(!player.isAlreadyPiaoFen())
                            player.writeSocket(msg);
                    }
                    isSendPiaoFenMsg = 1;
                }
                return false;
            }
        }else if(piaoFenType<=2){
            for (PdkPlayer player : playerMap.values()) {
                player.setAlreadyPiaoFen(true);
                player.setPiaoFen(piaoFenType);
                for (PdkPlayer p : playerMap.values()) {
                    p.writeComMessage(WebSocketMsgType.res_code_pdk_broadcast_piaofen, (int)player.getUserId(),player.getPiaoFen());
                }
            }
        }
        return true;
    }

    public synchronized void piaoFen(PdkPlayer player,int fen){
        if (piaoFenType<3||player.isAlreadyPiaoFen())
            return;
        if(fen<=8&&fen>=0)
            player.setPiaoFen(fen);
        player.setAlreadyPiaoFen(true);
        StringBuilder sb = new StringBuilder("pdk");
        sb.append("|").append(getId());
        sb.append("|").append(getPlayBureau());
        sb.append("|").append(player.getUserId());
        sb.append("|").append(player.getSeat());
        sb.append("|").append(player.isAutoPlay() ? 1 : 0);
        sb.append("|").append("piaoFen").append("|").append(fen);
        LogUtil.msgLog.info(sb.toString());
        int confirmTime=0;
        for (Map.Entry<Integer, PdkPlayer> entry : seatMap.entrySet()) {
            entry.getValue().writeComMessage(WebSocketMsgType.res_code_pdk_broadcast_piaofen, (int)player.getUserId(),player.getPiaoFen());
            if(entry.getValue().isAlreadyPiaoFen())
                confirmTime++;
        }
        if (confirmTime == max_player_count) {
            checkDeal(player.getUserId());
        }
    }

    @Override
    public boolean canQuit(Player player) {
        if (state == table_state.play || playedBureau > 0 || isMatchRoom() || isGoldRoom()) {
            return false;
        } else if(state == table_state.ready ){
            if(isSendPiaoFenMsg==1){
                return false;
            }
            return true;
        }else {
            return true;
        }
    }


    /**
     * ????????????:???4??????
     *
     * @param player
     * @param cards
     * @return true???4?????????false?????????
     */
    public boolean checkIsChai4Zha(PdkPlayer player, List<Integer> cards) {
        if (chai4Zha == 1) {
            // ?????????????????????
            return false;
        }
        List<Integer> handPais = new ArrayList<>(player.getHandPais());
        Map<Integer, Integer> map = CardTool.loadCards(handPais);
        List<Integer> valListOf4Zha = new ArrayList<>();
        for (Map.Entry<Integer, Integer> kv : map.entrySet()) {
            if (kv.getValue() >= 4) {
                valListOf4Zha.add(kv.getKey());
            }else if(AAAZha==1 && kv.getKey() == 14 && kv.getValue() == 3){
                // 3???A?????????
                valListOf4Zha.add(kv.getKey());
            }
        }
        if (valListOf4Zha.size() == 0) {
            return false;
        }
        boolean isChai4Zha = false;
        for (Integer card : cards) {
            int val = CardTool.loadCardValue(card);
            if (valListOf4Zha.contains(val)) {
                isChai4Zha = true;
            }
        }
        if (!isChai4Zha) {
            return false;
        }
        CardUtils.Result cardResult = CardUtils.calcCardValue(CardUtils.loadCards(cards), siDai,isFirstCardType32==1,AAAZha==1);
        if (cardResult.getType() == 4 && siDai > 0) {
            // 4?????????????????????
            return false;
        }else if(cardResult.getType() == 100){
            // ????????????????????????
            return false;
        }else if(cardResult.getType() == 1000){
            // 3???A??????????????????
            return false;
        }
        return true;
    }

    public boolean isNewRound() {
        return newRound;
    }

    public void setNewRound(boolean newRound) {
        this.newRound = newRound;
        changeExtend();
    }

    @Override
    public int getDissPlayerAgreeCount() {
        return getPlayerCount();
    }

    public int getAAAZha() {
        return AAAZha;
    }

    public void setAAAZha(int AAAZha) {
        this.AAAZha = AAAZha;
    }

    public String getTableMsg() {

        Map<String, Object> json = new HashMap<>();
        json.put("wanFa", "?????????");
        if (isGroupRoom()) {
            json.put("roomName", getRoomName());
        }
        json.put("playerCount", getPlayerCount());
        json.put("count", getTotalBureau());
        if (autoPlay) {
            json.put("autoTime", autoTimeOut2 / 1000);
            if (autoPlayGlob == 1) {
                json.put("autoName", "??????");
            } else {
                json.put("autoName", "??????");
            }
        }
        return JSON.toJSONString(json);
    }

    /**
     * ????????????
     *
     * @param player
     * @param cards
     * @return true:????????????????????????????????????????????????????????????false:??????????????????
     */
    public boolean checkBaoDan(PdkPlayer player, List<Integer> cards) {
        if (cards == null || cards.size() > 1) {
            return false;
        }
        int nextSeat = getNextSeat(player.getSeat());
        PdkPlayer nextPlayer = seatMap.get(nextSeat);
        if (nextPlayer == null || nextPlayer.getHandPais().size() != 1) {
            return false;
        }
        int card = cards.get(0);
        for (Integer handPai : player.getHandPais()) {
            if (handPai % 100 > card % 100) {
                return true;
            }
        }
        return false;
    }

    private Map<Integer, List<List<Integer>>> getAllPlayOutCard() {
        Map<Integer, List<List<Integer>>> outCardsMap = new HashMap<>();
        for (PdkPlayer player : playerMap.values()) {
            outCardsMap.put(player.getSeat(), player.getOutPais());
        }
        return outCardsMap;
    }

    private List<Integer> getAllPdkCards() {
        return CardTool.getAllPdkCards(playType);
    }

    public List<Integer> robotChupai(PdkPlayer player, List<Integer> disCards, boolean nextDan) {
        long start = System.currentTimeMillis();
        List<Integer> curList = new ArrayList<>(player.getHandPais());
        List<Integer> residueList = CardTypeTool.getAllResidueCards(curList, getAllPdkCards(), getAllPlayOutCard());

        IBTExecutor robotAI =  RobotManager.generateRobotAI(playType, player.getRobotAILevel());

        StringBuilder sb = new StringBuilder("Pdk|robot");
        sb.append("|").append(getId());
        sb.append("|").append(getPlayBureau());
        sb.append("|").append(player.getUserId());
        sb.append("|").append(player.getSeat());
        sb.append("|").append(player.isAutoPlay() ? 1 : 0);
        sb.append("|").append(curList);
        sb.append("|").append(disCards);

        robotAI.getRootContext().clear();
        robotAI.getRootContext().setVariable("handCards", new ArrayList<>(curList));
        robotAI.getRootContext().setVariable("initHandCards", new ArrayList<>(curList));
        robotAI.getRootContext().setVariable("disCards", disCards);
        robotAI.getRootContext().setVariable("residueCards", residueList);
        robotAI.getRootContext().setVariable("nextDan", nextDan ? 1 : 0);
        int step = 1;
        do {
            robotAI.tick();
            if (step++ > 200) {
                break;
            }
        } while (robotAI.getRootContext().getVariable("chupai") == null);
        robotAI.terminate();
        List<Integer> chupai;
        if (robotAI.getRootContext().getVariable("chupai") == null) {
            chupai = null;
            sb.append("|").append(step);
            sb.append("|").append("noDisCard");
        } else {
            chupai = (List<Integer>) robotAI.getRootContext().getVariable("chupai");
            sb.append("|").append(step);
            sb.append("|").append(chupai);
        }
        long timeUse = System.currentTimeMillis() - start;
        if (timeUse >= 0) {
            sb.append("|").append(timeUse);
            LogUtil.robot.info(sb.toString());
        }
        return chupai;
    }

    @Override
    public synchronized void checkRobotPlay() {
        // ????????????
        if (state == table_state.ready) {
            for (PdkPlayer player : seatMap.values()) {
                if (player.isRobot()) {
                    player.calcRobotActionRND(4, 4);
                    player.addRobotActionCounter();
                    if (player.canRobotAction()) {
                        autoReady(player);
                        player.resetRobotActionCounter();
                    }
                }
            }
            return;
        }
        // ????????????
        if (tableStatus == PdkConstants.TABLE_STATUS_DANIAO) {
            for (PdkPlayer player : seatMap.values()) {
                if (player.isRobot() && player.getNiaoFen() != -1) {
                    player.calcRobotActionRND(4, 4);
                    player.addRobotActionCounter();
                    if (player.canRobotAction()) {
                        daNiao(player, 0);
                        player.resetRobotActionCounter();
                    }
                }
            }
            return;
        } else if (daNiaoFen == 0 && piaoFenType > 0 && isSendPiaoFenMsg == 1 && finishFapai == 0) {
            //???????????????????????????
            for (PdkPlayer player : seatMap.values()) {
                if (player.isRobot() && !player.isAlreadyPiaoFen()) {
                    player.calcRobotActionRND(4, 4);
                    player.addRobotActionCounter();
                    if (player.canRobotAction()) {
                        piaoFen(player, piaoFenType >= 3 ? 0 : piaoFenType);
                        player.resetRobotActionCounter();
                    }
                }
            }
            return;
        }

        if (state != table_state.play) {
            return;
        }
        PdkPlayer player = seatMap.get(getNextDisCardSeat());
        if (player == null) {
            return;
        }

        if (!player.isRobot()) {
            return;
        }

        // ?????????????????????
        if (isFirstChuPai()) {
            // ????????????
            player.calcRobotActionRND(5, 2);
        } else {
            if (isNewRound()) {
                // ???????????????
                player.calcRobotActionRND(2, 2);
            } else {
                // ??????
                player.calcRobotActionRND(2, 2);
            }
        }
        player.addRobotActionCounter();
        if (!player.canRobotAction()) {
            return;
        }

        // ????????????3??????????????????3????????????3??????????????????????????????????????????
        if (checkHeiTao3ChuTou(player)) {
            return;
        }
        List<Integer> curList = new ArrayList<>(player.getHandPais());
        if (curList.isEmpty()) {
            return;
        }

        List<Integer> oppo;
        if (disCardSeat != player.getSeat()) {
            oppo = getNowDisCardIds();
            if (oppo != null) {
                oppo = new ArrayList<>(oppo);
            }
        } else {
            oppo = null;
        }

        boolean nextDan = seatMap.get(calcNextSeat(player.getSeat())).getHandPais().size() == 1;
        List<Integer> list = null;
        list = robotChupai(player, oppo, nextDan);
        if (list == null) {
            list = CardTypeTool.getBestAI2(curList, oppo, nextDan, this);
        }
        if (oppo != null && oppo.size() > 0 && oppo.size() % 5 == 0 && list != null && list.size() % 5 != 0 && card3Eq == 1) {
            CardUtils.Result result = CardUtils.calcCardValue(CardUtils.loadCards(list), siDai, isFirstCardType32 == 1, AAAZha == 1);
            if (result.getType() == 3 || result.getType() == 33) {
                list = null;
            }
        }
        player.resetRobotActionCounter();
        playCommand(player, list);
        player.setAutoPlay(false, this);
        player.setLastOperateTime(System.currentTimeMillis());
    }

    public boolean isFirstChuPai() {
        return getDisCardRound() == 0;
    }

}
