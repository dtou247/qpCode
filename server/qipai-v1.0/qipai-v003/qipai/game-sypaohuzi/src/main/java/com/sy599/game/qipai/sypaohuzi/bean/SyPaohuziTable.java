package com.sy599.game.qipai.sypaohuzi.bean;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import com.sy599.game.common.bean.CreateTableInfo;
import com.sy599.game.db.bean.gold.GoldRoomConfig;
import com.sy599.game.msg.serverPacket.TableMjResMsg;
import com.sy599.game.msg.serverPacket.TablePhzResMsg;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sy599.game.GameServerConfig;
import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.UserResourceType;
import com.sy599.game.common.constant.LangMsg;
import com.sy599.game.common.constant.SharedConstants;
import com.sy599.game.common.constant.SharedConstants.player_state;
import com.sy599.game.common.constant.SharedConstants.table_state;
import com.sy599.game.db.bean.DataStatistics;
import com.sy599.game.db.bean.PlayLogTable;
import com.sy599.game.db.bean.TableInf;
import com.sy599.game.db.bean.UserExtend;
import com.sy599.game.db.bean.UserGroupPlaylog;
import com.sy599.game.db.bean.UserPlaylog;
import com.sy599.game.db.bean.gold.GoldRoom;
import com.sy599.game.db.dao.DataStatisticsDao;
import com.sy599.game.db.dao.TableDao;
import com.sy599.game.db.dao.TableLogDao;
import com.sy599.game.db.dao.UserDao;
import com.sy599.game.db.dao.gold.GoldRoomDao;
import com.sy599.game.manager.TableManager;
import com.sy599.game.msg.serverPacket.ComMsg.ComRes;
import com.sy599.game.msg.serverPacket.PlayCardResMsg.PlayPaohuziRes;
import com.sy599.game.msg.serverPacket.TablePhzResMsg.ClosingPhzInfoRes;
import com.sy599.game.msg.serverPacket.TablePhzResMsg.ClosingPhzPlayerInfoRes;
import com.sy599.game.msg.serverPacket.TableRes.CreateTableRes;
import com.sy599.game.msg.serverPacket.TableRes.DealInfoRes;
import com.sy599.game.msg.serverPacket.TableRes.PlayerInTableRes;
import com.sy599.game.qipai.sypaohuzi.constant.PaohuziConstant;
import com.sy599.game.qipai.sypaohuzi.constant.PaohzCard;
import com.sy599.game.qipai.sypaohuzi.rule.PaohuziIndex;
import com.sy599.game.qipai.sypaohuzi.rule.PaohuziMingTangRule;
import com.sy599.game.qipai.sypaohuzi.rule.PaohzCardIndexArr;
import com.sy599.game.qipai.sypaohuzi.rule.RobotAI;
import com.sy599.game.qipai.sypaohuzi.tool.PaohuziHuLack;
import com.sy599.game.qipai.sypaohuzi.tool.PaohuziResTool;
import com.sy599.game.qipai.sypaohuzi.tool.PaohuziTool;
import com.sy599.game.staticdata.KeyValuePair;
import com.sy599.game.udplog.UdpLogger;
import com.sy599.game.util.DataMapUtil;
import com.sy599.game.util.GameConfigUtil;
import com.sy599.game.util.GameUtil;
import com.sy599.game.util.JacksonUtil;
import com.sy599.game.util.JsonWrapper;
import com.sy599.game.util.LogUtil;
import com.sy599.game.util.PayConfigUtil;
import com.sy599.game.util.ResourcesConfigsUtil;
import com.sy599.game.util.SendMsgUtil;
import com.sy599.game.util.StringUtil;
import com.sy599.game.util.TimeUtil;
import com.sy599.game.websocket.constant.WebSocketMsgType;

public class SyPaohuziTable extends BaseTable {
	/*** ??????map */
    private Map<Long, SyPaohuziPlayer> playerMap = new ConcurrentHashMap<>();
	/*** ????????????????????? */
    private Map<Integer, SyPaohuziPlayer> seatMap = new ConcurrentHashMap<>();
    /**
	 * ??????????????????
	 **/
    private volatile List<Integer> startLeftCards = new ArrayList<>();
    /**
	 * ??????????????????
	 **/
    private volatile List<PaohzCard> leftCards = new ArrayList<>();
	/*** ??????flag */
    private volatile int moFlag;
	/*** ??????????????????flag */
    private volatile int toPlayCardFlag;
    private volatile PaohuziCheckCardBean autoDisBean;
    private volatile int moSeat;
    private volatile PaohzCard zaiCard;
    private volatile PaohzCard beRemoveCard;
    private volatile int maxPlayerCount = 3;
    private volatile List<Integer> huConfirmList = new ArrayList<>();
	/*** ???????????????????????? */
    private volatile KeyValuePair<Integer, Integer> moSeatPair;
	/*** ???????????????????????? */
    private volatile KeyValuePair<Integer, Integer> checkMoMark;
    private volatile int sendPaoSeat;
    private volatile boolean firstCard = true;
    private volatile int shuXingSeat = 0;
    private volatile int ceiling =0;
    /**
	 * 0??? 1??? 2??? 3??? 4??? 5??? 6??????
	 */
    private Map<Integer, List<Integer>> actionSeatMap = new ConcurrentHashMap<>();
    private volatile List<PaohzCard> nowDisCardIds = new ArrayList<>();
	// ?????????
    private volatile int isRedBlack;
	// ?????????
    private volatile int isLianBanker;
	// ??????????????? 3???????????? 5????????????
    private volatile int xiTotun;

	private volatile int catCardCount = 0;// ??????????????????
	// ???????????????
    private List<Integer> chouCards=new ArrayList<>();
    /**
	 * ????????????
	 */
	private volatile int autoTimeOut = Integer.MAX_VALUE;
	private volatile int autoTimeOut2 = Integer.MAX_VALUE;
	// ???????????????0??????1???
    private int jiaBei;
	// ?????????????????????xx???????????????
    private int jiaBeiFen;
	// ????????????????????????
    private int jiaBeiShu;
    
	/** ??????1????????????2????????? */
    private int autoPlayGlob;

	private int autoTableCount;

    private volatile int timeNum = 0;

    /**
	 * ?????????????????????????????? ??????????????????????????????????????? 1??????????????????????????????????????? ??????????????????????????? ??????????????????
	 * 2???????????????????????????????????????????????????????????????????????????????????????????????? ?????????????????????????????????????????? ?????????????????????????????????????????????
	 */
    private Map<Integer, TempAction> tempActionMap = new ConcurrentHashMap<>();
	// ?????????????????????
    private int chui=-1;
	// ??????????????????
    private int finishFapai=0;
    //??????below??????
    private int belowAdd=0;
    private int below=0;
    //?????????
    private int zaiTiHu=0;
    //??????
    private int paoHu=0;

    public boolean getFirstCard(){
    	return firstCard;
    }
    public int getXiTotun() {
		return xiTotun;
	}

	public void setXiTotun(int xiTotun) {
		this.xiTotun = xiTotun;
	}

	public int getIsRedBlack() {
		return isRedBlack;
	}

	public void setIsRedBlack(int isRedBlack) {
		this.isRedBlack = isRedBlack;
	}

	public int getIsLianBanker() {
		return isLianBanker;
	}

	public void setIsLianBanker(int isLianBanker) {
		this.isLianBanker = isLianBanker;
	}

    public int getCeiling() {
        return ceiling;
    }

    public void setCeiling(int ceiling) {
        this.ceiling = ceiling;
        changeExtend();
    }

    public int getFinishFapai() {
        return finishFapai;
    }

    public int getChui() {
        return chui;
    }

    public void setChui(int chui) {
        this.chui = chui;
    }

    public int getPaoHu() {
        return paoHu;
    }

    public void setPaoHu(int paoHu) {
        this.paoHu = paoHu;
    }

    public void setFinishFapai(int finishFapai) {
        this.finishFapai = finishFapai;
    }

    /**
	 * ????????????????????????
	 */
    public List<Integer> getStartLeftCards() {
        return startLeftCards;
    }

    @Override
    public boolean ready(Player player) {
        boolean flag=super.ready(player);
        if(chui!=1||playedBureau>0)
            return flag;
        int count=0;
        for(SyPaohuziPlayer p:seatMap.values()){
            if (p.getState() == player_state.ready||p.getState() == player_state.play)
                count++;
        }
        if(count==getMaxPlayerCount()){
            for(SyPaohuziPlayer p:seatMap.values()){
                p.setChui(-1);
            }
        }
        return flag;
    }

    @Override
    public boolean isAllReady() {
        if (getPlayerCount() < getMaxPlayerCount()) {
            return false;
        }
        for (Player player : getSeatMap().values()) {
            if(!player.isRobot()){
                if(chui==1){
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
        if (chui==1&&playedBureau==0) {
            boolean chuiOver = true;
            for (SyPaohuziPlayer player : playerMap.values()) {
                if(player.getChui()<0){
                    chuiOver = false;
                    break;
                }
            }
            if(!chuiOver){
                if (finishFapai==0) {
                    LogUtil.msgLog.info("bopi|sendChui|" + getId() + "|" + getPlayBureau());
                    ComRes msg = SendMsgUtil.buildComRes(WebSocketMsgType.res_code_sybp_chui).build();
                    for (SyPaohuziPlayer player : playerMap.values()) {
                        if(player.getChui()<0&&player.getIsSendChui()==0){
                            player.writeSocket(msg);
                            player.setIsSendChui(1);
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    public synchronized void chui(SyPaohuziPlayer player,int isChui){
        if (chui!=1||player.getChui()!=-1)
            return;
        player.setChui(isChui);
        StringBuilder sb = new StringBuilder("bopi");
        sb.append("|").append(getId());
        sb.append("|").append(getPlayBureau());
        sb.append("|").append(player.getUserId());
        sb.append("|").append(player.getSeat());
        sb.append("|").append(player.isAutoPlay() ? 1 : 0);
        sb.append("|").append("chui");
        LogUtil.msgLog.info(sb.toString());
        int confirmTime=0;
        for (Map.Entry<Integer, SyPaohuziPlayer> entry : seatMap.entrySet()) {
            entry.getValue().writeComMessage(WebSocketMsgType.res_code_sybp_broadcast_chui, (int)player.getUserId(),player.getChui());
            if(entry.getValue().getChui()!=-1)
                confirmTime++;
        }
        if (confirmTime == maxPlayerCount) {
            checkDeal(player.getUserId());
            startNext();
        }
    }


    @Override
    public void initExtend0(JsonWrapper wrapper) {
        String hu = wrapper.getString(1);
        if (!StringUtils.isBlank(hu)) {
            huConfirmList = StringUtil.explodeToIntList(hu);
        }
        moFlag = wrapper.getInt(2, 0);
        toPlayCardFlag = wrapper.getInt(3, 0);
        moSeat = wrapper.getInt(4, 0);
        String moSeatVal = wrapper.getString(5);
        if (!StringUtils.isBlank(moSeatVal)) {
            moSeatPair = new KeyValuePair<>();
            String[] values = moSeatVal.split("_");
            String idStr = StringUtil.getValue(values, 0);
            if (!StringUtil.isBlank(idStr)) {
                moSeatPair.setId(Integer.parseInt(idStr));
            }

            moSeatPair.setValue(StringUtil.getIntValue(values, 1));
        }
        String autoDisPhz = wrapper.getString(6);
        if (!StringUtils.isBlank(autoDisPhz)) {
            autoDisBean = new PaohuziCheckCardBean();
            autoDisBean.initAutoDisData(autoDisPhz);
        }
        zaiCard = PaohzCard.getPaohzCard(wrapper.getInt(7, 0));
        sendPaoSeat = wrapper.getInt(8, 0);
        firstCard = wrapper.getInt(9, 1) == 1 ? true : false;
        beRemoveCard = PaohzCard.getPaohzCard(wrapper.getInt(10, 0));
        shuXingSeat = wrapper.getInt(11, 0);
        maxPlayerCount = wrapper.getInt(12, 3);
        startLeftCards = loadStartLeftCards(wrapper.getString("startLeftCards"));
        ceiling = wrapper.getInt(13, 0);
        isRedBlack = wrapper.getInt(14, 0);
        isLianBanker = wrapper.getInt(15, 0);
        xiTotun = wrapper.getInt(16, 3);
        if (payType== -1) {
            String isAAStr =  wrapper.getString("isAAConsume");
            if (!StringUtils.isBlank(isAAStr)) {
                this.payType = Boolean.parseBoolean(wrapper.getString("isAAConsume"))?1:2;
            } else {
                payType=1;
            }
        }

        catCardCount = wrapper.getInt("catCardCount", catCardCount);

        jiaBei = wrapper.getInt(17, 0);
        jiaBeiFen = wrapper.getInt(18, 0);
        jiaBeiShu = wrapper.getInt(19, 0);
        
        autoPlayGlob = wrapper.getInt(20, 0);
        autoTimeOut = wrapper.getInt(21, 0);
        if(autoPlay && autoTimeOut <=1) {
        	autoTimeOut= 60000;
        }
        autoTimeOut2 =autoTimeOut;

        tempActionMap = loadTempActionMap(wrapper.getString("22"));
        chui = wrapper.getInt(23, -1);
        finishFapai = wrapper.getInt(24, 0);
        below = wrapper.getInt(25, 0);
        belowAdd = wrapper.getInt(26, 0);
        paoHu = wrapper.getInt(27, 0);
		// TODO ??????????????????????????????????????????????????????,??????????????????????????????????????????????????????
        // -----------------start------------------------------
        if (finishFapai == 0) {
			// ?????????????????????????????????
            for (SyPaohuziPlayer player : seatMap.values()) {
                if (player.getHandPais() != null && player.getHandPais().size() > 0) {
                    finishFapai = 1;
                    break;
                }
            }
        }
        // -----------------end------------------------------
    }

    private List<Integer> loadStartLeftCards(String json) {
        List<Integer> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        JSONArray jsonArray = JSONArray.parseArray(json);
        for (Object val : jsonArray) {
            list.add(Integer.valueOf(val.toString()));
        }
        return list;
    }

    @Override
    public <T> T getPlayer(long id, Class<T> cl) {
        return (T) playerMap.get(id);
    }

    @Override
    protected void initNowAction(String nowAction) {
        JsonWrapper wrapper = new JsonWrapper(nowAction);
        String val1 = wrapper.getString(1);
        if (!StringUtils.isBlank(val1)) {
            actionSeatMap = DataMapUtil.toListMap(val1);
        }
    }

    @Override
    protected String buildNowAction() {
        JsonWrapper wrapper = new JsonWrapper("");
        wrapper.putString(1, DataMapUtil.explodeListMap(actionSeatMap));
        return wrapper.toString();
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
    public void calcOver() {
        if (getState() != table_state.play) {
            return;
        }
        if(getPlayBureau() >= getMaxPlayerCount()){
            changeTableState(table_state.over);
        }
        boolean isHuangZhuang = false;
        List<Integer> winList = new ArrayList<>(huConfirmList);
        if (winList.size() == 0 && leftCards.size() == 0) {
			// ??????
            isHuangZhuang = true;
        }
        List<Integer> mt = null;
        int winFen = 0;
		int totalTun = 0;// ????????????????????????
        boolean isOver = playBureau == totalBureau;
        Map<Long,Integer> outScoreMap = new HashMap<>();
        Map<Long,Integer> ticketMap = new HashMap<>();
        for (int winSeat : winList) {
            // ????????????
            boolean isSelfMo = winSeat == moSeat;
            SyPaohuziPlayer winPlayer = seatMap.get(winSeat);
            int getPoint = 0;
            winPlayer.changeAction(PaohuziConstant.ACTION_COUNT_INDEX_HU, 1);
            if (isSelfMo) {
                winPlayer.changeAction(PaohuziConstant.ACTION_COUNT_INDEX_ZIMO, 1);
            }
            mt = PaohuziMingTangRule.calcMingTang(winPlayer);
            winFen = PaohuziMingTangRule.calcMingTangFen(winPlayer, mt);
            if (isBoPi()) {
                // ???????????????
                getPoint = totalTun + winFen;
                if (totalBureau != 1) { // ????????????????????????50???
                    totalBureau = 50;
                }
            } else {
                // ???????????????
                totalTun = winPlayer.calcHuPoint(winFen, getXiTotun());// ?????????
            }
            for (int seat : seatMap.keySet()) {
                if (!winList.contains(seat)) {
                    SyPaohuziPlayer player = seatMap.get(seat);
                    if (isBoPi()) {
                        if (4 == getMaxPlayerCount() && seat == shuXingSeat) {
                            // ???????????????
                            continue;
                        }
                        player.calcResult(this, 1, 0, isHuangZhuang);
                    } else {
                        getPoint += totalTun;
                        player.calcResult(this, 1, -totalTun, isHuangZhuang);
                    }
                }
            }
            winPlayer.calcResult(this, 1, getPoint, isHuangZhuang);
            if (isSiRenBoPi()) {
                SyPaohuziPlayer shuXingPlayer = seatMap.get(shuXingSeat);
                int shuXingAddPoint = getPoint / 2;
                if (getPoint % 2 != 0) {
                    shuXingAddPoint += 1;
                }
                shuXingPlayer.calcResult(this, 1, shuXingAddPoint, isHuangZhuang);
            }
        }
        SyPaohuziPlayer winPlayer = null;
        //boolean selfMo = false;
        if (!winList.isEmpty()) {
            winPlayer = seatMap.get(winList.get(0));
            //selfMo = winPlayer.getSeat() == moSeat;
        } else {
            SyPaohuziPlayer lastWinPlayer = seatMap.get(lastWinSeat);
            if (isBoPi()) {
                lastWinPlayer.calcResult(this, 1, -10, isHuangZhuang);
                if (4 == getMaxPlayerCount()) {
                    SyPaohuziPlayer shuXingPlayer = seatMap.get(shuXingSeat);
                    shuXingPlayer.calcResult(this, 1, 10, isHuangZhuang);
                }
            }
        }
        if (isBoPi()) {
            if (!winList.isEmpty()) {
                int winSeat = winList.get(0);
                if (winSeat == lastWinSeat && getIsLianBanker() == 1) { // ??????
                    SyPaohuziPlayer bankerPlayer;
                    bankerPlayer = winPlayer == null ? seatMap.get(lastWinSeat) : winPlayer;
                    if (ceiling > 0 && bankerPlayer.getTotalPoint() >= ceiling) {
                        isOver = true;
                    }
                } else {
                    // ??????????????????????????????>=100?????????
                    for (SyPaohuziPlayer temPlayer : seatMap.values()) {
                        if (temPlayer.getTotalPoint() >= 100) {
                            isOver = true;
                            break;
                        }
                    }
                }
            } else if (isSiRenBoPi()) {
                // ???????????????100?????????
                SyPaohuziPlayer shuXingPlayer = seatMap.get(shuXingSeat);
                if (shuXingPlayer.getTotalPoint() >= 100) {
                    isOver = true;
                }
            }
            if (!isOver && playBureau >= totalBureau) {
                isOver = true;
            }
        } else {
            isOver = playBureau >= totalBureau;
        }

        if(autoPlayGlob >0) {
			// //????????????
            boolean diss = false;
            if(autoPlayGlob ==1) {
            	 for (SyPaohuziPlayer seat : seatMap.values()) {
                 	if(seat.isAutoPlay()) {
                     	diss = true;
                     	break;
                     }
                     
                 }
			} else if (autoPlayGlob == 3 && isBoPi()) {
				diss = checkAuto3();
			}
            if(diss) {
            	 autoPlayDiss= true;
            	 isOver =true;
            }
        }

        boolean isBoPi = isBoPi();
        for (SyPaohuziPlayer player : seatMap.values()) {
            int bopiPint = getBopiPoint(player);
            player.setWinLossPoint(bopiPint);// ?????????
            player.setTotalPoint(isOver && isBoPi ? randNumber(player.getTotalPoint()) : player.getTotalPoint());// ????????????
        }

        if(isOver){
            calcPointBeforeOver();
        }

        // -----------?????????---------------------------------
        if (isGoldRoom()) {
            for (SyPaohuziPlayer player : seatMap.values()) {
                player.setPoint(player.getTotalPoint());
                if (isBoPi) {
                    player.setWinGold(player.getWinLossPoint());
                    player.setWinLossPoint((int) (player.getWinLossPoint() * goldRoom.getRate()));
                }else {
                    player.setWinGold(player.getTotalPoint());
                }
            }
            calcGoldRoom();
        }

        // -----------solo------------------
        if (isSoloRoom()) {
            for (SyPaohuziPlayer player : seatMap.values()) {
                if (isBoPi) {
                    if (winList.size() == 0) {
                        // ??????????????????
                        if (player.getSeat() != lastWinSeat) {
                            player.setSoloWinner(true);
                        } else {
                            player.setSoloWinner(false);
                        }
                    } else {
                        if (player.getSeat() == winList.get(0)) {
                            player.setSoloWinner(true);
                        } else {
                            player.setSoloWinner(false);
                        }
                    }
                } else {
                    if (player.getTotalPoint() > 0) {
                        player.setSoloWinner(true);
                    } else {
                        player.setSoloWinner(false);
                    }
                }
            }
            calcSoloRoom();
        }

        
        ClosingPhzInfoRes.Builder res = sendAccountsMsg(isOver, winList, winFen, mt, totalTun, false, outScoreMap,ticketMap);
        saveLog(isOver,0L, res.build());
        if (!winList.isEmpty()) {
            setLastWinSeat(winList.get(0));
        } else {
            if (!isBoPi()) {
                int next = getNextSeat(lastWinSeat);
                setLastWinSeat(next);
            }
        }
        calcAfter();
        if (isOver) {
            // ?????????????????????AA????????????
            if (isBoPi() && totalBureau == 1) {
                if (isFirstBureauOverConsume()) {
                    consume();
                }
            }

            calcOver1();
            calcOver2();
            calcOver3();
            diss();
        } else {
            initNext();
            calcOver1();
        }
        for (Player player : seatMap.values()) {
            player.saveBaseInfo();
        }
    }

    public void calcPointBeforeOver() {
        // ?????????????????????
        boolean isBoPi = isBoPi();
        if (isBoPi) {
            for (SyPaohuziPlayer player : seatMap.values()) {
                player.setWinLossPoint(0);// ?????????
            }
            for (int i = 1; i < getPlayerCount() + 1; i++) {
                for (int j = i + 1; j < getPlayerCount() + 1; j++) {
                    if (i != j) {
                        countBetweenTwoPoint(seatMap.get(i), seatMap.get(j));
                    }
                }
            }
        }

        // ----------????????????????????????----------
        if (jiaBei == 1) {
            int jiaBeiPoint = 0;
            int loserCount = 0;
            if (isBoPi) {
                for (SyPaohuziPlayer player : seatMap.values()) {
                    if (player.getWinLossPoint() > 0 && player.getWinLossPoint() < jiaBeiFen) {
                        jiaBeiPoint += player.getWinLossPoint() * (jiaBeiShu - 1);
                        player.setWinLossPoint(player.getWinLossPoint() * jiaBeiShu);
                    } else if (player.getWinLossPoint() < 0) {
                        loserCount++;
                    }
                }
                if (jiaBeiPoint > 0) {
                    for (SyPaohuziPlayer player : seatMap.values()) {
                        if (player.getWinLossPoint() < 0) {
                            player.setWinLossPoint(player.getWinLossPoint() - (jiaBeiPoint / loserCount));
                        }
                    }
                }
            } else {
                for (SyPaohuziPlayer player : seatMap.values()) {
                    if (player.getTotalPoint() > 0 && player.getTotalPoint() < jiaBeiFen) {
                        jiaBeiPoint += player.getTotalPoint() * (jiaBeiShu - 1);
                        player.setTotalPoint(player.getTotalPoint() * jiaBeiShu);
                    } else if (player.getTotalPoint() < 0) {
                        loserCount++;
                    }
                }
                if (jiaBeiPoint > 0) {
                    for (SyPaohuziPlayer player : seatMap.values()) {
                        if (player.getTotalPoint() < 0) {
                            player.setTotalPoint(player.getTotalPoint() - (jiaBeiPoint / loserCount));
                        }
                    }
                }
            }
        }

        // ----------???????????????below???+belowAdd???-------------
        if (belowAdd > 0 && playerMap.size() == 2) {
            if (isBoPi) {
                for (SyPaohuziPlayer player : seatMap.values()) {
                    int totalPoint = player.getWinLossPoint();
                    if (totalPoint > -below && totalPoint < 0) {
                        player.setWinLossPoint(player.getWinLossPoint() - belowAdd);
                    } else if (totalPoint < below && totalPoint > 0) {
                        player.setWinLossPoint(player.getWinLossPoint() + belowAdd);
                    }
                }
            }
        }
    }

    private boolean checkAuto3() {
		boolean diss = false;
		// if(autoPlayGlob==3) {
		boolean diss2 = false;
		for (SyPaohuziPlayer seat : seatMap.values()) {
			if (seat.isAutoPlay()) {
				diss2 = true;
				break;
			}
		}
		if (diss2) {
			autoTableCount += 1;
		} else {
			autoTableCount = 0;
		}
		if (autoTableCount == 3) {
			diss = true;
		}
		// }
		return diss;
	}

    @Override
    public void saveLog(boolean over,long winId, Object resObject) {
        ClosingPhzInfoRes res = (ClosingPhzInfoRes) resObject;
        LogUtil.d_msg("tableId:" + id + " play:" + playBureau + " over:" + res);
        String logRes = JacksonUtil.writeValueAsString(LogUtil.buildClosingInfoResLog(res));
        String logOtherRes = JacksonUtil.writeValueAsString(LogUtil.buildClosingInfoResOtherLog(res));
        Date now = TimeUtil.now();
        UserPlaylog userLog = new UserPlaylog();
        userLog.setLogId(playType);
        userLog.setUserId(creatorId);
        userLog.setTableId(id);
        userLog.setRes(extendLogDeal(logRes));
        userLog.setTime(now);
        userLog.setTotalCount(totalBureau);
        userLog.setCount(playBureau);
        userLog.setStartseat(lastWinSeat);
        userLog.setOutCards(playLog);
        userLog.setExtend(logOtherRes);
        userLog.setType(creditMode == 1 ? 2 : 1 );
        userLog.setGeneralExt(buildGeneralExtForPlaylog().toString());
        long logId = TableLogDao.getInstance().save(userLog);
        saveTableRecord(logId, over, playBureau);
        UdpLogger.getInstance().sendSnapshotLog(masterId, playLog, logRes);
        if (!isGoldRoom()){
        	for (SyPaohuziPlayer player : playerMap.values()) {
                player.addRecord(logId, playBureau);
            }
        }
    }

    @Override
    protected void loadFromDB1(TableInf info) {
        if (!StringUtils.isBlank(info.getNowDisCardIds())) {
            this.nowDisCardIds = PaohuziTool.explodePhz(info.getNowDisCardIds(), ",");
        }
        if (!StringUtils.isBlank(info.getLeftPais())) {
            this.leftCards = PaohuziTool.explodePhz(info.getLeftPais(), ",");
        }
    }

    @Override
    protected void sendDealMsg() {
        sendDealMsg(0);
    }

    @Override
    protected void sendDealMsg(long userId) {
		// ??????????????????
//        int lastCardIndex = RandomUtils.nextInt(21);
        SyPaohuziPlayer winPlayer = seatMap.get(lastWinSeat);

        for (SyPaohuziPlayer tablePlayer : seatMap.values()) {
            DealInfoRes.Builder res = DealInfoRes.newBuilder();
            res.addAllHandCardIds(tablePlayer.getSeat() == shuXingSeat?winPlayer.getHandPais():tablePlayer.getHandPais());
            res.setNextSeat(lastWinSeat);
			res.setGameType(getWanFa());// 1????????? 2??????
            res.setRemain(leftCards.size());
            res.setBanker(lastWinSeat);
            res.addXiaohu(winPlayer.getHandPais().get(0));
            if (isSiRenBoPi()) {
                res.addXiaohu(shuXingSeat);
            }
            if(tablePlayer.isAutoPlay()) {
       		 addPlayLog(tablePlayer.getSeat(), PaohzDisAction.action_tuoguan + "",1 + "");
            }
            tablePlayer.writeSocket(res.build());
        }
    }

    @Override
    public synchronized void startNext() {
        checkAction();
    }

    public void play(SyPaohuziPlayer player, List<Integer> cardIds, int action) {
        play(player, cardIds, action, false, false, false);
    }

    private void hu(SyPaohuziPlayer player, List<PaohzCard> cardList, int action, PaohzCard nowDisCard) {
        if (!actionSeatMap.containsKey(player.getSeat())) {
            return;
        }
        if (huConfirmList.contains(player.getSeat())) {
            return;
        }
        if (!checkAction(player, action,cardList,nowDisCard)) {
            player.writeComMessage(WebSocketMsgType.res_com_code_temp_action_skip);
            // player.writeErrMsg(LangMsgEnum.code_29);
            return;
        }
        List<Integer> actionList = actionSeatMap.get(player.getSeat());
        if (actionList.get(0) != 1) {
            return;
        }
        PaohuziHuLack paoHu = player.checkPaoHu(nowDisCard, isSelfMo(player), (isBoPi() && firstCard));
        PaohuziHuLack pingHu = player.checkHu(nowDisCard, isSelfMo(player));
        if (!pingHu.isHu()) {
            PaohuziCheckCardBean paoHuBean = player.checkPaoHu();
            if (paoHuBean.isHu()) {
                pingHu = paoHuBean.getLack();
            }
        }
        PaohuziHuLack hu = pingHu;
        if (paoHu.isHu()) {
            int paoHuOutHuXi = player.getPaoOutHuXi(nowDisCard, isSelfMo(player), (isBoPi() && firstCard));
            if (paoHu.getHuxi() + paoHuOutHuXi > pingHu.getHuxi()+player.getOutHuxi()) {
                play(player, PaohuziTool.toPhzCardIds(paoHu.getPaohuList()), paoHu.getPaohuAction(), false, true, false);
                hu = player.checkHu(null, isSelfMo(player));
            }
        } else {
            hu = pingHu;
        }

        if (hu.isHu()&&player.getSiShou()==0) {
			// broadMsg(player.getName() + " ??????");
            player.setHuxi(hu.getHuxi());
            player.setHu(hu);
            huConfirmList.add(player.getSeat());
            addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
            sendActionMsg(player, action, null, PaohzDisAction.action_type_action);
            calcOver();
        } else {
			broadMsg(player.getName() + " ????????????");
        }

    }

    /**
	 * ????????????
	 *
	 * @param player
	 * @return
	 */
    public boolean isSelfMo(SyPaohuziPlayer player) {
        if (moSeatPair != null) {
            return moSeatPair.getValue().intValue() == player.getSeat() || (player.getSeat()==shuXingSeat&&moSeatPair.getValue().intValue()==lastWinSeat);
        }
        return false;
    }

    /**
	 * ???
	 */
    private void ti(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action, boolean moPai) {
		// cards?????????4????????????
        if (cardList == null) {
			System.out.println("????????????:" + cardList);
			player.writeErrMsg("????????????:" + cardList);
            return;
        }

        if (cardList.size() == 1) {
            List<PaohzCard> tiCards = player.getTiCard(cardList.get(0));
            if (tiCards == null || tiCards.size() != 3) {
				System.out.println("????????????:" + tiCards);
				player.writeErrMsg("????????????:" + tiCards);
                return;
            }
            cardList.addAll(tiCards);
        } else {
            if (!player.getHandPhzs().contains(cardList.get(0))) {
                return;
            }
        }
		// ????????????
        boolean isZaiPao = player.isZaiPao(cardList.get(0).getVal());

        if (cardList.size() != 4 && !cardList.contains(nowDisCard)) {
            cardList.add(0, nowDisCard);
        }

        if (cardList.size() != 4) {
            return;
        }

        if (!PaohuziTool.isSameCard(cardList)) {
			System.out.println("????????????:" + cardList);
			player.writeErrMsg("????????????:" + cardList);
            return;
        }
		player.changeAction(PaohuziConstant.ACTION_COUNT_INDEX_TI, 1);
        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
        if (nowDisCard != null) {
            getDisPlayer().removeOutPais(nowDisCard);
        }
        player.disCard(action, cardList);
        clearAction();
        setAutoDisBean(null);

        PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, false);
        if (checkAutoDis != null) {
            playAutoDisCard(checkAutoDis, true);
        }

		// ?????????????????????
        PaohuziCheckCardBean checkCard = player.checkPaoHu();
        checkPaohuziCheckCard(checkCard);

		// ???????????????
        if (!moPai) {
			// ??????????????????????????????????????????
            boolean disCard = setDisPlayer(player, action, checkCard.isHu());
            if (!disCard) {
                // checkMo();
            }

        }
        sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action, isZaiPao, false);
        // if (player.isFangZhao()) {
        // LogUtil.msgLog.info("----tableId:" + getId() + "---userName:" +
		// player.getName() + "------???-----??????????????????" + cardList.get(0));
        // player.setFangZhao(0);
        // // player.writeComMessage(WebSocketMsgType.res_code_phz_fangzhao,
        // player.getUserId() + "", 0 + "");
        // relieveFangZhao(player.getUserId());
        // }

    }

    /**
	 * ???(??????)
	 *
	 * @param cardList
	 *            ????????????
	 */
    private void zai(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action) {
        if (!actionSeatMap.containsKey(player.getSeat())) {
            return;
        }

        boolean isFristDisCard = player.isFristDisCard();
        PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, false);
        if (checkAutoDis != null) {
            playAutoDisCard(checkAutoDis, true);
        }
        getDisPlayer().removeOutPais(nowDisCard);
        if (!cardList.contains(nowDisCard)) {
            cardList.add(0, nowDisCard);
        }
        setBeRemoveCard(nowDisCard);
        if (action == PaohzDisAction.action_zai) {
            setZaiCard(nowDisCard);

        }
        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
        player.disCard(action, cardList);
        clearAction();
        setAutoDisBean(null);
		// ?????????????????????
        PaohuziCheckCardBean checkCard = player.checkPaoHu();
        checkPaohuziCheckCard(checkCard);
		// ???????????????
        boolean disCard = setDisPlayer(player, action, isFristDisCard, checkCard.isHu());
        sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action);
        // if (player.isFangZhao()) {
        // LogUtil.msgLog.info("----tableId:" + getId() + "---userName:" +
		// player.getName() + "------???-----??????????????????" + cardList.get(0));
        // player.setFangZhao(0);
        // // player.writeComMessage(WebSocketMsgType.res_code_phz_fangzhao,
        // player.getUserId() + "", 0 + "");
        // relieveFangZhao(player.getUserId());
        // }
        if (!disCard) {
            // checkMo();
        }

    }

    /**
	 * ???
	 */
    private void pao(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action, boolean isHu, boolean isPassHu) {
        if (cardList.size() != 3 && cardList.size() != 1) {
			broadMsg("??????????????????:" + cardList);
            return;
        }
        List<Integer> actionList = actionSeatMap.get(player.getSeat());
        if (actionList == null) {
            return;
        }
        if (!isHu && actionList.get(5) != 1) {
            return;
        }

		// ???????????????????????? ???????????????
        if (!isHu && !checkAction(player, action,cardList,nowDisCard)) {
			// ??????????????????
			// ????????????????????????
            if (actionList.get(0) == 1) {
                actionList.set(0, 0);
                addAction(player.getSeat(), actionList);
				// ??????????????????
                setSendPaoSeat(player.getSeat());
                sendPlayerActionMsg(player);
            }
            // player.writeErrMsg(LangMsgEnum.code_29);
            return;
        }
        boolean isZaiPao = player.isZaiPao(cardList.get(0).getVal());
        getDisPlayer().removeOutPais(nowDisCard);
        if (!cardList.contains(nowDisCard)) {
            cardList.add(0, nowDisCard);
        }
        setBeRemoveCard(nowDisCard);

        if (cardList.size() == 1) {
			// ???????????????????????????????????????????????????
            List<PaohzCard> list = player.getSameCards(nowDisCard);
            cardList.addAll(list);
        }

        if (cardList.size() != 4) {
            return;
        }

		// ??????????????????
        boolean isFristDisCard = player.isFristDisCard();
        PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, false);
        if (checkAutoDis != null) {
            playAutoDisCard(checkAutoDis, true);
        }
		player.changeAction(PaohuziConstant.ACTION_COUNT_INDEX_PAO, 1);
        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
        player.disCard(action, cardList);
        clearAction();
        setAutoDisBean(null);

        if (!isHu && !isPassHu && isMoFlag()) {
            PaohuziCheckCardBean checkCard = player.checkPaoHu();
            checkPaohuziCheckCard(checkCard);
        }

		// ???????????????
        if (!isHu) {
            boolean disCard = setDisPlayer(player, action, isFristDisCard, false);
            sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action, isZaiPao, !disCard);
            if (!disCard) {
                if (PaohuziConstant.isAutoMo) {
                    checkMo();
                }
            }
        } else {
            sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action, isZaiPao, false);
        }

        // if (player.isFangZhao()) {
        // LogUtil.msgLog.info("----tableId:" + getId() + "---userName:" +
		// player.getName() + "------???-----??????????????????" + cardList.get(0));
        // player.setFangZhao(0);
        // // player.writeComMessage(WebSocketMsgType.res_code_phz_fangzhao,
        // player.getUserId() + "", 0 + "");
        // relieveFangZhao(player.getUserId());
        // }

    }

    private void relieveFangZhao(long userId) {
        for (Player player : seatMap.values()) {
            player.writeComMessage(WebSocketMsgType.res_code_phz_fangzhao, userId + "", 0 + "");
        }
    }

    /**
	 * ??????
	 */
    private void disCard(SyPaohuziPlayer player, List<PaohzCard> cardList, int action) {
        if (!actionSeatMap.isEmpty()) {
        	player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
			LogUtil.e("??????:" + JacksonUtil.writeValueAsString(actionSeatMap));
            return;
        }

        if (toPlayCardFlag != 1) {
    		player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
			LogUtil.e(player.getName() + "?????? toPlayCardFlag:" + toPlayCardFlag + "??????");
            checkMo();
            return;
        }

        if (player.getSeat() != nowDisCardSeat) {
        	player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
			player.writeErrMsg("??????:" + nowDisCardSeat + "??????");
            return;
        }
        if (cardList.size() != 1) {
        	player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
			player.writeErrMsg("??????????????????:" + cardList);
            return;
        }

        PaohuziHandCard cardBean = player.getPaohuziHandCard();
        if (!cardBean.isCanoperateCard(cardList.get(0))) {
        	player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
			player.writeErrMsg("??????????????????:" + cardList);
			LogUtil.e("??????????????????:" + cardList);
            return;
        }

		// ?????????????????????
        boolean paoFlag = isFangZhao(player, cardList.get(0));
        if (paoFlag) {
			if (player.isAutoPlay()) {// ??????????????????
        		player.setFangZhao(1);
        		for (Player playerTemp : getSeatMap().values()) {
    				playerTemp.writeComMessage(WebSocketMsgType.res_code_phz_fangzhao, player.getUserId() + "", 1 + "");
    			}
        	}else if (!player.isFangZhao() && !player.isRobot()) {
            	player.writeComMessage(WebSocketMsgType.res_code_phz_dis_err, cardList.get(0).getId());
                player.writeComMessage(WebSocketMsgType.res_com_code_fangzhao, cardList.get(0).getId());
                return;
            }
        }

		// ???????????????????????????????????????
        boolean canDiHu = false;
        if (player.isFristDisCard() && player.getSeat() == lastWinSeat && isBoPi()) {
            canDiHu = true;
        } else {
            if (firstCard) {
                firstCard = false;
            }
        }

        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
		checkFreePlayerTi(player, action);// ???????????????
        player.disCard(action, cardList);
        setMoFlag(0);
        markMoSeat(player.getSeat(), action);
        clearMoSeatPair();
		setToPlayCardFlag(0); // ??????????????????flag
        setDisCardSeat(player.getSeat());
        setNowDisCardIds(cardList);
        setNowDisCardSeat(getNextDisCardSeat());
        PaohuziCheckCardBean autoDisCard = checkDisAction(player, action, cardList.get(0), canDiHu);
        sendActionMsg(player, action, cardList, PaohzDisAction.action_type_dis);
        // if (autoDisCard != null) {
		// // ??????????????????
        // playAutoDisCard(autoDisCard);
        // } else {
        checkAutoMo();
        // }
    }

    private void checkAutoMo() {
        if (isTest()) {
            checkMo();

        }
    }

    private void tiLong(SyPaohuziPlayer player) {
        boolean isTiLong = false;
        List<PaohzCard> cardList = new ArrayList<>();
        while (player.getOweCardCount() < -1) {
            if (!isTiLong) {
                isTiLong = true;
                removeAction(player.getSeat());
            }
            PaohzCard card = null;
            if (GameServerConfig.isDebug()) {
                if (card == null) {
                    card = getNextCard(106);
                }
                if (card == null) {
                    card = getNextCard(4);
                }
            }

            if (card == null) {
                card = getNextCard();

            }
            player.tiLong(card);
            cardList.add(card);

            addPlayLog(player.getSeat(), PaohzDisAction.action_buPai + "", (card == null ? 0 : card.getId()) + "");
            StringBuilder sb = new StringBuilder("SyPhz");
            sb.append("|").append(getId());
            sb.append("|").append(getPlayBureau());
            sb.append("|").append(player.getUserId());
            sb.append("|").append(player.getSeat());
            sb.append("|").append(player.isAutoPlay() ? 1 : 0);
            sb.append("|").append("tiLong");
            sb.append("|").append(card);
            LogUtil.msgLog.info(sb.toString());
        }

        if (isTiLong) {
            sendActionMsg(player, PaohzDisAction.action_tilong, cardList, PaohzDisAction.action_type_action, false, false);

            PaohuziCheckCardBean checkCard = player.checkCard(null, true, true, false);
            if (checkPaohuziCheckCard(checkCard)) {
                playAutoDisCard(checkCard);
                if (player.getSeat() != lastWinSeat && checkCard.isTi()) {
                    player.setOweCardCount(player.getOweCardCount() - 1);
                }
                tiLong(player);
            }
        }
    }

    public void checkFreePlayerTi(SyPaohuziPlayer player, int action) {
        if (player.getSeat() == lastWinSeat && player.isFristDisCard() && action != PaohzDisAction.action_ti) {
            for (int seat : getSeatMap().keySet()) {
                if (lastWinSeat == seat) {
                    continue;
                }
                SyPaohuziPlayer nowPlayer = seatMap.get(seat);
                PaohuziCheckCardBean checkCard = nowPlayer.checkCard(null, true, true, false);
                if (checkPaohuziCheckCard(checkCard)) {
                    playAutoDisCard(checkCard);
                    if (nowPlayer.isFristDisCard()) {
                        nowPlayer.setFristDisCard(false);
                    }
                    tiLong(nowPlayer);
					/*-- ??????????????????????????????
					boolean needBuPai = false;
					if (checkCard.isTi()) {
						PaohuziHandCard cardBean = nowPlayer.getPaohuziHandCard();
						PaohzCardIndexArr valArr = cardBean.getIndexArr();
						PaohuziIndex index3 = valArr.getPaohzCardIndex(3);
						if (index3 != null && index3.getLength() >= 2) {
							needBuPai = true;
						}
					}
					playAutoDisCard(checkCard);
					if (nowPlayer.isFristDisCard()) {
						nowPlayer.setFristDisCard(false);
					}
					
					if (needBuPai) {
						PaohzCard buPai = leftCards.remove(0);
						for (SyPaohuziPlayer tempPlayer : seatMap.values()) {
							if (seat == tempPlayer.getSeat()) {
								tempPlayer.getHandPhzs().add(buPai);
								System.out.println("----------------------------------??????:" + tempPlayer.getName() + "  ?????????:" + buPai.getId() + "  ????????????:" + leftCards.size());
							}
							tempPlayer.writeComMessage(WebSocketMsgType.res_com_code_phzbupai, seat, buPai.getId(), leftCards.size());
						}
					}
					 */

                }
                checkSendActionMsg();
            }
        }
    }

    /**
	 * ???
	 */
    private void peng(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action) {
        if (!actionSeatMap.containsKey(player.getSeat())) {
            return;
        }
        if (!checkAction(player, action, cardList, nowDisCard)) {
            player.writeComMessage(WebSocketMsgType.res_com_code_temp_action_skip);
            return;
        }

        cardList = player.getPengList(nowDisCard, cardList);
        if (cardList == null) {
			player.writeErrMsg("?????????");
            return;
        }
        if (!cardList.contains(nowDisCard)) {
            cardList.add(0, nowDisCard);
        }
        setBeRemoveCard(nowDisCard);

        boolean isFristDisCard = player.isFristDisCard();
        PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, false);
        if (checkAutoDis != null) {
            playAutoDisCard(checkAutoDis, true);

        }
        getDisPlayer().removeOutPais(nowDisCard);
        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
        player.disCard(action, cardList);
        clearAction();

        boolean disCard = setDisPlayer(player, action, isFristDisCard, false);
        sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action);
        if (!disCard) {
            // checkMo();
        }

		// ????????????,??????????????????????????????
        if (isMoFlag()) {
            for (SyPaohuziPlayer seatPlayer : seatMap.values()) {
                if (seatPlayer.getSeat() == player.getSeat()) {
                    continue;
                }
                seatPlayer.removePassChi(nowDisCard.getVal());
            }
        }
    }

    /**
	 * ???
	 */
    private void pass(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action) {
        if (!actionSeatMap.containsKey(player.getSeat())) {
			// player.writeErrMsg("???????????????????????????????????????");
            return;
        }
        List<Integer> actionList = actionSeatMap.get(player.getSeat());
        List<Integer> list = PaohzDisAction.parseToDisActionList(actionList);
		// ?????????????????????????????????
        if (list.contains(PaohzDisAction.action_zai) || list.contains(PaohzDisAction.action_ti) || list.contains(PaohzDisAction.action_pao) || list.contains(PaohzDisAction.action_chouzai)) {
            return;
        }
		// ????????????????????????????????????????????????
        if (!list.contains(PaohzDisAction.action_chi) && !list.contains(PaohzDisAction.action_peng) && !list.contains(PaohzDisAction.action_hu)) {
            return;
        }

		// ??????????????????????????????
        boolean isPassHu = actionList.get(0) == 1;
        if (actionList.get(0) == 1 && player.getHandPhzs().isEmpty()) {
			player.writeErrMsg("????????????????????????");
            return;
        }

        if(action==PaohzDisAction.action_pass){
            int logId;
            if(paoHu==1){
                logId=0;
            }else {
                logId = nowDisCard.getId();
            }
            addPlayLog(player.getSeat(), PaohzDisAction.action_guo + "",logId+"");
            setPaoHu(0);
        }

        if(player.getOperateCards().isEmpty()){
            player.setSiShou(1);
        }

        int val = 0;
        if (nowDisCard != null) {
            val = nowDisCard.getVal();
        }

        boolean addPassChi = false;
        if (player.getSeat() == moSeat) {
            addPassChi = true;
        }

		// ???pass?????????????????????passChi???passPeng???
        for (int passAction : list) {
            player.pass(passAction, val, addPassChi);

        }
        removeAction(player.getSeat());

		// ????????????
        if (autoDisBean != null) {
            refreshTempAction(player);
            playAutoDisCard(autoDisBean);
        } else {
            PaohuziCheckCardBean checkCard = player.checkCard(nowDisCard, isSelfMo(player), isPassHu, false, false, true);
            checkCard.setPassHu(isPassHu);
            boolean check = checkPaohuziCheckCard(checkCard);
            markMoSeat(player.getSeat(), action);
            sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action);
            if (check) {
                playAutoDisCard(checkCard, true);
            } else {
                if (PaohuziConstant.isAutoMo) {
                    checkMo();
                } else {
                    if (isTest()) {
                        checkMo();
                    }
                }
            }
            refreshTempAction(player);
        }

        if (this.leftCards.size() == 0 && !isHasSpecialAction()) {
            calcOver();
        }

    }

    /**
	 * ???
	 */
    private void chi(SyPaohuziPlayer player, List<PaohzCard> cardList, PaohzCard nowDisCard, int action) {
        List<Integer> actionList = actionSeatMap.get(player.getSeat());
        if (actionList == null) {
            return;
        }
        if (cardList != null) {
            if (cardList.size() % 3 != 0) {
				player.writeErrMsg("?????????" + cardList);
                return;
            }

            if (!cardList.contains(nowDisCard)) {
                return;
            }
        }

        cardList = player.getChiList(nowDisCard, cardList);
        if (cardList == null) {
			player.writeErrMsg("?????????");
            return;
        }

        if (cardList.size() > 3) {
            PaohuziHandCard card = player.getPaohuziHandCard();
            if (card.getOperateCards().size() <= cardList.size()) {
				player.writeErrMsg("?????????????????????????????????????????????");
                return;
            }
        }

        if (!checkAction(player, action,cardList,nowDisCard)) {
            player.writeComMessage(WebSocketMsgType.res_com_code_temp_action_skip);
			// ????????????????????????
            if (actionList.get(1) == 1) {
                actionList.set(1, 0);
				// ??????????????????????????????
                player.pass(PaohzDisAction.action_peng, nowDisCard.getVal());
//                addAction(player.getSeat(), actionList);
				// ??????????????????
//                sendPlayerActionMsg(player);
            }
            // player.writeErrMsg(LangMsgEnum.code_29);
            return;
        }

        if (PaohuziTool.isPaohuziRepeat(cardList)) {
			player.writeErrMsg("?????????");
            return;
        }

        boolean isFristDisCard = player.isFristDisCard();
        PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, false);
        if (checkAutoDis != null) {
            playAutoDisCard(checkAutoDis, true);
        }

        if (!cardList.contains(nowDisCard)) {
            cardList.add(0, nowDisCard);
        } else {
            cardList.remove(nowDisCard);
            cardList.add(0, nowDisCard);
        }
        setBeRemoveCard(nowDisCard);

        getDisPlayer().removeOutPais(nowDisCard);
        addPlayLog(player.getSeat(), action + "", PaohuziTool.implodePhz(cardList, ","));
        player.disCard(action, cardList);
        clearAction();

        boolean disCard = setDisPlayer(player, action, isFristDisCard, false);
        sendActionMsg(player, action, cardList, PaohzDisAction.action_type_action);
        if (!disCard) {
            if (PaohuziConstant.isAutoMo) {
                checkMo();
            }
        }

    }

    public synchronized void play(SyPaohuziPlayer player, List<Integer> cardIds, int action, boolean moPai, boolean isHu, boolean isPassHu) {
		// ??????play??????
        if (state != table_state.play || player.getSeat() == shuXingSeat) {
            return;
        }

        PaohzCard nowDisCard = null;
        List<PaohzCard> cardList = null;
		// ???????????????????????????????????????,??????????????????id????????????????????????
        if (action != PaohzDisAction.action_mo) {
            if (nowDisCardIds != null && nowDisCardIds.size() == 1) {
                nowDisCard = nowDisCardIds.get(0);
            }
            if (action != PaohzDisAction.action_pass) {
                if (!player.isCanDisCard(cardIds, nowDisCard)) {
                    return;
                }
            }
            if (cardIds != null && !cardIds.isEmpty()) {
                cardList = PaohuziTool.toPhzCards(cardIds);
            }
        }

        if (action != PaohzDisAction.action_mo) {
            StringBuilder sb = new StringBuilder("SyPhz");
            sb.append("|").append(getId());
            sb.append("|").append(getPlayBureau());
            sb.append("|").append(player.getUserId());
            sb.append("|").append(player.getSeat());
            sb.append("|").append(player.isAutoPlay() ? 1 : 0);
            sb.append("|").append(PaohzDisAction.getActionName(action));
            sb.append("|").append(cardList);
            sb.append("|").append(nowDisCard);
            if (actionSeatMap.containsKey(player.getSeat())) {
                sb.append("|").append(PaohuziCheckCardBean.actionListToString(actionSeatMap.get(player.getSeat())));
            }
            LogUtil.msgLog.info(sb.toString());
        }
        // //////////////////////////////////////////////////////

        if (action == PaohzDisAction.action_ti) {
            if (cardList.size() > 4) {
				// ????????????
                PaohzCardIndexArr arr = PaohuziTool.getMax(cardList);
                PaohuziIndex index = arr.getPaohzCardIndex(3);
                for (List<PaohzCard> tiCards : index.getPaohzValMap().values()) {
                    ti(player, tiCards, nowDisCard, action, moPai);
                }
            } else {
                ti(player, cardList, nowDisCard, action, moPai);
            }
        } else if (action == PaohzDisAction.action_hu) {
            hu(player, cardList, action, nowDisCard);
        } else if (action == PaohzDisAction.action_peng) {
            peng(player, cardList, nowDisCard, action);
        } else if (action == PaohzDisAction.action_chi) {
            chi(player, cardList, nowDisCard, action);
        } else if (action == PaohzDisAction.action_pass) {
            pass(player, cardList, nowDisCard, action);
        } else if (action == PaohzDisAction.action_pao) {
            pao(player, cardList, nowDisCard, action, isHu, isPassHu);
        } else if (action == PaohzDisAction.action_zai || action == PaohzDisAction.action_chouzai) {
            zai(player, cardList, nowDisCard, action);
        } else if (action == PaohzDisAction.action_mo) {
            if (isTest()) {
                return;
            }
            if (checkMoMark != null) {
                int cAction = cardIds.get(0);
                if (checkMoMark.getId() == player.getSeat() && checkMoMark.getValue() == cAction) {
                    checkMo();
                } /*
					 * else { // System.out.println("????????????-->" + player.getName()
					 * + // " seat:" + player.getSeat() + "-" +
					 * checkMoMark.getId() + // " action:" + cAction + "- " +
					 * checkMoMark.getValue()); }
					 */
            }

        } else {
            disCard(player, cardList, action);
        }
        if (!moPai && !isHu) {
			// ????????????????????????????????????
            robotDealAction();
        }

    }

    private boolean setDisPlayer(SyPaohuziPlayer player, int action, boolean isHu) {
        return setDisPlayer(player, action, false, isHu);
    }

    /**
	 * ????????????????????????
	 */
    private boolean setDisPlayer(SyPaohuziPlayer player, int action, boolean isFirstDis, boolean isHu) {
        if (this.leftCards.isEmpty()) {
			// ????????????????????????
            if (!isHu) {
                calcOver();
            }
            return false;
        }

        boolean canDisCard = true;
        if (player.getHandPhzs().isEmpty()) {
            canDisCard = false;

        } else if (player.getOperateCards().isEmpty()) {
            canDisCard = false;
            if(!isHu)
                player.setSiShou(1);
        }
        if (canDisCard && ((player.getSeat() == lastWinSeat && isFirstDis) || player.isNeedDisCard(action))) {
            setNowDisCardSeat(player.getSeat());
            setToPlayCardFlag(1);
            return true;
        } else {
			// ??????????????? ?????????????????????
            setToPlayCardFlag(0);
            player.compensateCard();
            int next = calcNextSeat(player.getSeat());
            setNowDisCardSeat(next);

            if (actionSeatMap.isEmpty()) {
                markMoSeat(player.getSeat(), action);
            }
            return false;
        }
    }

    /**
	 * ????????????????????????????????? ????????????????????????????????????????????????????????????
	 */
    private boolean checkAction(SyPaohuziPlayer player, int action, List<PaohzCard> cardList, PaohzCard nowDisCard) {
		// ???????????????????????????
        boolean canPlay = true;
        List<Integer> stopActionList = PaohzDisAction.findPriorityAction(action);
        for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()) {
            if (player.getSeat() != entry.getKey()) {
				// ??????
                boolean can = PaohzDisAction.canDis(stopActionList, entry.getValue());
                if (!can) {
                    canPlay = false;
                }
                List<Integer> disActionList = PaohuziDisAction.parseToDisActionList(entry.getValue());
                if (disActionList.contains(action)) {
					// ??????????????????????????? ????????????????????????
                    int actionSeat = entry.getKey();
                    int nearSeat = getNearSeat(disCardSeat, Arrays.asList(player.getSeat(), actionSeat));
                    if (nearSeat != player.getSeat()) {
                        canPlay = false;
                    }

                }
            }
        }
        if (canPlay) {
            clearTempAction();
            return true;
        }

        int seat = player.getSeat();
        tempActionMap.put(seat, new TempAction(seat, action, cardList, nowDisCard));

		// ?????????????????????????????????????????? ?????????????????????
        if (tempActionMap.size() > 0 && tempActionMap.size() == actionSeatMap.size()) {
            int maxAction = -1;
            int maxSeat = 0;
            Map<Integer, Integer> prioritySeats = new HashMap<>();
            int maxActionSize = 0;
            for (TempAction temp : tempActionMap.values()) {
                if (maxAction == -1 || PaohzDisAction.findPriorityAction(maxAction).contains(temp.getAction())) {
                    maxAction = temp.getAction();
                    maxSeat = temp.getSeat();
                }
                prioritySeats.put(temp.getSeat(), temp.getAction());
            }
            Set<Integer> maxPrioritySeats = new HashSet<>();
            for (int mActionSet : prioritySeats.keySet()) {
                if (prioritySeats.get(mActionSet) == maxAction) {
                    maxActionSize++;
                    maxPrioritySeats.add(mActionSet);
                }
            }
            if (maxActionSize > 1) {
                maxSeat = getNearSeat(disCardSeat, new ArrayList<>(maxPrioritySeats));
                maxAction = prioritySeats.get(maxSeat);
            }
            SyPaohuziPlayer tempPlayer = seatMap.get(maxSeat);
            List<PaohzCard> tempCardList = tempActionMap.get(maxSeat).getCardList();
            for (int removeSeat : prioritySeats.keySet()) {
                if (removeSeat != maxSeat) {
                    removeAction(removeSeat);
                }
            }
            clearTempAction();
			// ?????????????????????????????????
            play(tempPlayer, PaohuziTool.toPhzCardIds(tempCardList), maxAction);
        }else if(tempActionMap.size() + 1 == actionSeatMap.size() ){
			// ?????????????????????
            for(int s : actionSeatMap.keySet()){
                if(!tempActionMap.containsKey(s)){
                    List<Integer> list = actionSeatMap.get(s);
                    boolean isPao = list.get(5) == 1;
                    for(int i= 0 ;i < list.size() ;i++){
                        if(i != 5 && list.get(i) == 1 ){
                            isPao = false;
                        }
                    }
                    if(isPao){
						// ?????????
                        if (autoDisBean != null) {
                            playAutoDisCard(autoDisBean);
                        }
                    }
                }
            }
        }
        return canPlay;
    }

    /**
	 * ??????????????????????????????????????????????????????
	 *
	 * @param player
	 */
    private void refreshTempAction(SyPaohuziPlayer player) {
        tempActionMap.remove(player.getSeat());
		Map<Integer, Integer> prioritySeats = new HashMap<>();// ?????????????????????
        for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()) {
            int seat = entry.getKey();
            List<Integer> actionList = entry.getValue();
            List<Integer> list = PaohuziDisAction.parseToDisActionList(actionList);
            int priorityAction = PaohzDisAction.getMaxPriorityAction(list);
            prioritySeats.put(seat, priorityAction);
        }
        int maxPriorityAction = Integer.MAX_VALUE;
        int maxPrioritySeat = 0;
		boolean isSame = true;// ?????????????????????
        for (int seat : prioritySeats.keySet()) {
            if (maxPrioritySeat != Integer.MAX_VALUE && maxPrioritySeat != prioritySeats.get(seat)) {
                isSame = false;
            }
            if (prioritySeats.get(seat) < maxPriorityAction) {
                maxPriorityAction = prioritySeats.get(seat);
                maxPrioritySeat = seat;
            }
        }
        if (isSame) {
            maxPrioritySeat = getNearSeat(disCardSeat, new ArrayList<>(prioritySeats.keySet()));
        }
        Iterator<TempAction> iterator = tempActionMap.values().iterator();
        while (iterator.hasNext()) {
            TempAction tempAction = iterator.next();
            if (tempAction.getSeat() == maxPrioritySeat) {
                int action = tempAction.getAction();
                List<PaohzCard> tempCardList = tempAction.getCardList();
                SyPaohuziPlayer tempPlayer = seatMap.get(tempAction.getSeat());
                iterator.remove();
				// ?????????????????????????????????
                play(tempPlayer, PaohuziTool.toPhzCardIds(tempCardList), action);
                break;
            }
        }
        changeExtend();
    }


    private void clearTempAction() {
        if (!tempActionMap.isEmpty()) {
            tempActionMap.clear();
            changeExtend();
        }
    }


    /**
	 * ???????????????????????????
	 */
    private SyPaohuziPlayer getDisPlayer() {
        return seatMap.get(disCardSeat);
    }

    private void record(SyPaohuziPlayer player, int action, List<PaohzCard> cardList) {
    }

    @Override
    public int isCanPlay() {
        if (getPlayerCount() < getMaxPlayerCount()) {
            return 1;
        }
        // for (SyPaohuziPlayer player : seatMap.values()) {
        // if (player.getIsEntryTable() != PdkConstants.table_online) {
		// // ?????????????????????
        // broadIsOnlineMsg(player, player.getIsEntryTable());
        // return 2;
        // }
        // }
        return 0;
    }

    private synchronized void checkMo() {
        if (autoDisBean != null) {
            playAutoDisCard(autoDisBean);
        }

		// 0??? 1??? 2??? 3??? 4??? 5???
        if (!actionSeatMap.isEmpty()) {
            return;
        }
        if (nowDisCardSeat == 0) {
            return;
        }

		// // ????????????????????????
        SyPaohuziPlayer player = seatMap.get(nowDisCardSeat);
        // if (moSeat == player.getSeat()) {
        // if (moSeat == disCardSeat) {
        // broadMsg("moSeat == disCardSeat" + moSeat + " " + disCardSeat);
        // return;
        //
        // }
        // }

        if (toPlayCardFlag == 1) {
			// ?????????????????????
            return;
        }

        if (leftCards == null) {
            return;
        }
        if (this.leftCards.size() == 0 && !isHasSpecialAction()) {
            calcOver();
            return;
        }

        clearMarkMoSeat();

        // PaohzCard card = PaohzCard.getPaohzCard(59);
        // PaohzCard card = getNextCard();
        StringBuilder sb = new StringBuilder("SyPhz");
        sb.append("|").append(getId());
        sb.append("|").append(getPlayBureau());
        sb.append("|").append(player.getUserId());
        sb.append("|").append(player.getSeat());
        sb.append("|").append(player.isAutoPlay() ? 1 : 0);
        sb.append("|").append("moPai");
        PaohzCard card = null;
        if (player.getFlatId().startsWith("vkscz2855914")) {
            card = getNextCard(102);
            // if (card == null) {
            // card = PaohzCard.getPaohzCard(61);
            // }
            if (card == null) {
                card = getNextCard();
            }
        } else {
            if (GameServerConfig.isDebug() && !player.isRobot()) {
                if (zpMap.containsKey(player.getUserId()) && zpMap.get(player.getUserId()) > 0) {
                    List<PaohzCard> cardList = PaohuziTool.findPhzByVal(getLeftCards(), zpMap.get(player.getUserId()));
                    if (cardList != null && cardList.size() > 0) {
                        zpMap.remove(player.getUserId());
                        card = cardList.get(0);
                        getLeftCards().remove(card);
                    }
                }
            }

            if(null!=gmDebugVal && gmDebugVal.size()>0 &&player.getUserId() == gmDebugUserId ){
                if(isGroupRoom() && player.groupTableDebugPermission(groupId,GameUtil.play_type_bopi)){
                    try{
                        int val = gmDebugVal.get(0);
                        gmDebugVal.remove(0);
                        List<PaohzCard> cardList = PaohuziTool.findPhzByVal(getLeftCards(), val );
                        if (cardList != null && cardList.size() > 0) {
                            card = cardList.get(0);
                            getLeftCards().remove(card);
                            sb.append("debug|");
                        }
                    }catch (Exception e){
                        card= null;
                    }
                }
            }

            if(card == null){
                card = getNextCard();
            }
        }

        addPlayLog(player.getSeat(), PaohzDisAction.action_mo + "", (card == null ? 0 : card.getId()) + "");

        sb.append("|").append(card);
        LogUtil.msgLog.info(sb.toString());

        if (card != null) {
            if (isTest()) {
                sleep();

            }
            // PaohuziCheckCardBean checkAutoDis = checkAutoDis(player, true);
            // if (checkAutoDis != null) {
            // playAutoDisCard(checkAutoDis, true);
            //
            // }

            setMoFlag(1);
            setMoSeat(player.getSeat());
            markMoSeat(card, player.getSeat());
            player.moCard(card);
            setDisCardSeat(player.getSeat());
            setFirstCard(false);
            setNowDisCardIds(new ArrayList<>(Arrays.asList(card)));
            setNowDisCardSeat(getNextDisCardSeat());

            PaohuziCheckCardBean autoDisCard = null;
            for (Entry<Integer, SyPaohuziPlayer> entry : seatMap.entrySet()) {
                PaohuziCheckCardBean checkCard = entry.getValue().checkCard(card, entry.getKey() == player.getSeat(), false);
                if (checkPaohuziCheckCard(checkCard)) {
                    autoDisCard = checkCard;
                }

            }

            markMoSeat(player.getSeat(), PaohzDisAction.action_mo);
            if (autoDisCard != null && autoDisCard.getAutoAction() == PaohzDisAction.action_zai) {
                sendMoMsg(player, PaohzDisAction.action_mo, new ArrayList<>(Arrays.asList(card)), PaohzDisAction.action_type_mo);

            } else {
                sendActionMsg(player, PaohzDisAction.action_mo, new ArrayList<>(Arrays.asList(card)), PaohzDisAction.action_type_mo);

            }

            if (autoDisBean != null) {
                playAutoDisCard(autoDisBean);
            }

            if (this.leftCards != null && this.leftCards.size() == 0 && !isHasSpecialAction()) {
                calcOver();
                return;
            }

//			if (PaohuziConstant.isAutoMo) {
            // if (actionSeatMap.isEmpty()) {
            // markMoSeat(player.getSeat(), PaohzDisAction.action_mo);
            // }
            // checkMo();
//			}
            checkAutoMo();
        }
    }

    /**
	 * ??????????????????????????????????????????
	 */
    private boolean isHasSpecialAction() {
        boolean b = false;
        for (List<Integer> actionList : actionSeatMap.values()) {
            if (actionList.get(0) == 1 || actionList.get(2) == 1 || actionList.get(3) == 1 || actionList.get(5) == 1 || actionList.get(6) == 1) {
				// ??????????????????????????????????????????
                b = true;
                break;
            }
        }
        return b;
    }

    /**
	 * @return ?????????????????????????????????
	 */
    private PaohuziCheckCardBean checkDisAction(SyPaohuziPlayer player, int action, PaohzCard disCard, boolean isFirstCard) {
        PaohuziCheckCardBean autoDisCheck = null;
        for (Entry<Integer, SyPaohuziPlayer> entry : seatMap.entrySet()) {
            if (entry.getKey() == player.getSeat()) {
                continue;
            }

//            PaohuziCheckCardBean checkCard = entry.getValue().checkCard(disCard, false, isFirstCard);
            PaohuziCheckCardBean checkCard = entry.getValue().checkCard(disCard,false,!isFirstCard,false,isFirstCard,false);
            boolean check = checkPaohuziCheckCard(checkCard);
            if (check) {
                autoDisCheck = checkCard;
            }
        }
        return autoDisCheck;
    }

    private boolean isFangZhao(SyPaohuziPlayer player, PaohzCard disCard) {

        for (Entry<Integer, SyPaohuziPlayer> entry : seatMap.entrySet()) {
            if (entry.getKey() == player.getSeat()) {
                continue;
            }

            boolean flag = entry.getValue().canFangZhao(disCard);
            if (flag) {
                return true;
            }
        }
        return false;
    }

    /**
	 * ???????????????
	 */
    private PaohuziCheckCardBean checkAutoDis(SyPaohuziPlayer player, boolean isMoPaiIng) {
        PaohuziCheckCardBean checkCard = player.checkTi();
        checkCard.setMoPaiIng(isMoPaiIng);
        boolean check = checkPaohuziCheckCard(checkCard);
        if (check) {
            return checkCard;
        } else {
            return null;
        }
    }

    public boolean checkPaohuziCheckCard(PaohuziCheckCardBean checkCard) {
        List<Integer> list = checkCard.getActionList();
        if (list == null || list.isEmpty()) {
            return false;
        }

        addAction(checkCard.getSeat(), list);
        List<PaohzCard> autoDisList = checkCard.getAutoDisList();
        if (autoDisList != null) {
			// ????????????????????????
            if (!checkCard.isHu()) {
                setAutoDisBean(checkCard);
                return true;
            }
        }
        return false;

    }

    public void setAutoDisBean(PaohuziCheckCardBean autoDisBean) {
        this.autoDisBean = autoDisBean;
        changeExtend();
    }

    private void addAction(int seat, List<Integer> actionList) {
        actionSeatMap.put(seat, actionList);
        addPlayLog(seat, PaohzDisAction.action_hasaction + "", StringUtil.implode(actionList));
        saveActionSeatMap();
    }

    private List<Integer> removeAction(int seat) {
        if (sendPaoSeat == seat) {
            setSendPaoSeat(0);
        }
        List<Integer> list = actionSeatMap.remove(seat);
        saveActionSeatMap();
        return list;
    }

    private void clearAction() {
        setSendPaoSeat(0);
        actionSeatMap.clear();
        saveActionSeatMap();
    }

    private void clearHuList() {
        huConfirmList.clear();
        changeExtend();
    }

    public void saveActionSeatMap() {
        dbParamMap.put("nowAction", JSON_TAG);
    }

    private void sendActionMsg(SyPaohuziPlayer player, int action, List<PaohzCard> cards, int actType) {
        sendActionMsg(player, action, cards, actType, false, false);
    }

    /**
	 * ????????????????????????msg
	 *
	 * @param player
	 * @param action
	 * @param cards
	 * @param actType
	 */
    private void sendMoMsg(SyPaohuziPlayer player, int action, List<PaohzCard> cards, int actType) {
        PlayPaohuziRes.Builder builder = PlayPaohuziRes.newBuilder();
        builder.setAction(action);
        builder.setUserId(player.getUserId() + "");
        builder.setSeat(player.getSeat());
        builder.setHuxi(player.getOutHuxi() + player.getZaiHuxi());
        // builder.setNextSeat(nowDisCardSeat);
        setNextSeatMsg(builder);
        builder.setRemain(leftCards.size());
        builder.addAllPhzIds(PaohuziTool.toPhzCardIds(cards));
        builder.setActType(actType);
        sendMoMsgBySelfAction(builder, player.getSeat());
    }

    /**
	 * ?????????????????????msg
	 */
    private void sendPlayerActionMsg(SyPaohuziPlayer player) {
        if (!actionSeatMap.containsKey(player.getSeat())) {
            return;
        }
        PlayPaohuziRes.Builder builder = PlayPaohuziRes.newBuilder();
        builder.setAction(PaohzDisAction.action_refreshaction);
        builder.setUserId(player.getUserId() + "");
        builder.setSeat(player.getSeat());
        builder.setHuxi(player.getOutHuxi() + player.getZaiHuxi());
        // builder.setNextSeat(nowDisCardSeat);
        setNextSeatMsg(builder);
        if (leftCards != null) {
            builder.setRemain(leftCards.size());

        }
        // builder.addAllPhzIds(PaohuziTool.toPhzCardIds(nowDisCardIds));
        builder.setActType(0);
        KeyValuePair<Boolean, Integer> zaiKeyValue = getZaiOrTiKeyValue();
        List<Integer> actionList = getSendSelfAction(zaiKeyValue, player.getSeat(), actionSeatMap.get(player.getSeat()));
        if (actionList != null) {
            builder.addAllSelfAct(actionList);
        }
        player.writeSocket(builder.build());

        if (player.getSeat()==lastWinSeat&&shuXingSeat>0){
            SyPaohuziPlayer paohuziPlayer = seatMap.get(shuXingSeat);
            paohuziPlayer.writeSocket(builder.build());
        }
    }

    private void setNextSeatMsg(PlayPaohuziRes.Builder builder) {
        // if (!GameServerConfig.isDebug()) {
        // builder.setNextSeat(nowDisCardSeat);
        //
        // } else {
        builder.setTimeSeat(nowDisCardSeat);
        if (toPlayCardFlag == 1) {
            builder.setNextSeat(nowDisCardSeat);
        } else {
            builder.setNextSeat(0);

        }

        // }

    }

    /**
	 * ????????????msg
	 *
	 * @param player
	 * @param action
	 * @param cards
	 * @param actType
	 */
    private void sendActionMsg(SyPaohuziPlayer player, int action, List<PaohzCard> cards, int actType, boolean isZaiPao, boolean isChongPao) {
        PlayPaohuziRes.Builder builder = PlayPaohuziRes.newBuilder();
        builder.setAction(action);
        builder.setUserId(player.getUserId() + "");
        builder.setSeat(player.getSeat());
        builder.setHuxi(player.getOutHuxi() + player.getZaiHuxi());
        setNextSeatMsg(builder);
        if (leftCards != null) {
            builder.setRemain(leftCards.size());

        }
        builder.addAllPhzIds(PaohuziTool.toPhzCardIds(cards));
        builder.setActType(actType);
        if (isZaiPao) {
            builder.setIsZaiPao(1);
        }
        if (isChongPao) {
            builder.setIsChongPao(1);
        }
        sendMsgBySelfAction(builder);
    }

    /**
	 * ????????????????????????????????????????????????
	 *
	 * @return
	 */
    private KeyValuePair<Boolean, Integer> getZaiOrTiKeyValue() {
        KeyValuePair<Boolean, Integer> keyValue = new KeyValuePair<>();
        boolean isHasZaiOrTi = false;
        int zaiSeat = 0;
        for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()) {
            if (entry.getValue().get(2) == 1 || entry.getValue().get(3) == 1) {
                isHasZaiOrTi = true;
                zaiSeat = entry.getKey();
                break;
            }
        }
        keyValue.setId(isHasZaiOrTi);
        keyValue.setValue(zaiSeat);
        return keyValue;
    }

    private List<Integer> getSendSelfAction(KeyValuePair<Boolean, Integer> zaiKeyValue, int seat, List<Integer> actionList) {
        boolean isHasZaiOrTi = zaiKeyValue.getId();
        int zaiSeat = zaiKeyValue.getValue();
        if (isHasZaiOrTi) {
            if (zaiSeat == seat) {
                return actionList;
            }
        } else if (actionList.get(0) == 1) {
            return actionList;
        } else if (actionList.get(5) == 1) {
            if (sendPaoSeat == seat) {
                return actionList;
            }
        } else if (actionList.get(2) == 1 || actionList.get(3) == 1) {
			// 0??? 1??? 2??? 3??? 4??? 5???
			// ??????????????????????????? ???????????????
            // ...
            return null;
        } else {
            return actionList;
        }
        return null;

    }

    /**
	 * ??????????????????????????????
	 *
	 * @param builder
	 */
    private void sendMoMsgBySelfAction(PlayPaohuziRes.Builder builder, int seat) {
        KeyValuePair<Boolean, Integer> zaiKeyValue = getZaiOrTiKeyValue();
        SyPaohuziPlayer winPlayer = seatMap.get(lastWinSeat);
        for (SyPaohuziPlayer player : seatMap.values()) {
            PlayPaohuziRes.Builder copy = builder.clone();
            if (player.getSeat() != seat) {
                // copy.clearPhzIds();
                // copy.addPhzIds(0);
                if (seat==lastWinSeat&&player.getSeat()==shuXingSeat){
                    copy.setHuxi(winPlayer.getOutHuxi() + winPlayer.getZaiHuxi());
                }
            } else {
                copy.setHuxi(player.getOutHuxi() + player.getZaiHuxi());
            }
            if (actionSeatMap.containsKey(player.getSeat())) {
                List<Integer> actionList = getSendSelfAction(zaiKeyValue, player.getSeat(), actionSeatMap.get(player.getSeat()));
                if (actionList != null) {
                    copy.addAllSelfAct(actionList);
                }
            }else if (seat==lastWinSeat&&shuXingSeat==player.getSeat()&&actionSeatMap.containsKey(winPlayer.getSeat())) {
                List<Integer> actionList = getSendSelfAction(zaiKeyValue, winPlayer.getSeat(), actionSeatMap.get(winPlayer.getSeat()));
                if (actionList != null) {
                    copy.addAllSelfAct(actionList);
                }
            }
            player.writeSocket(copy.build());
        }
    }

    /**
	 * ??????????????????????????????
	 *
	 * @param builder
	 */
    private void sendMsgBySelfAction(PlayPaohuziRes.Builder builder) {
        KeyValuePair<Boolean, Integer> zaiKeyValue = getZaiOrTiKeyValue();

        int actType = builder.getActType();
        boolean noShow = false;
        // boolean hasHu = false;
        int paoSeat = 0;
        if (PaohzDisAction.action_type_dis == actType || PaohzDisAction.action_type_mo == actType) {
            for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()) {
                if (1 == entry.getValue().get(5)) {
                    noShow = true;
                    paoSeat = entry.getKey();
                }

                // if (1 == entry.getValue().get(0)) {
                // hasHu = true;
                // }
            }

            // if (hasHu) {
            // noShow = false;
            // }
        }

        SyPaohuziPlayer winPlayer = seatMap.get(lastWinSeat);

        for (SyPaohuziPlayer player : seatMap.values()) {
            PlayPaohuziRes.Builder copy = builder.clone();
            if (copy.getSeat() == player.getSeat()) {
                copy.setHuxi(player.getOutHuxi() + player.getZaiHuxi());
                if(player.isAutoPlay() && copy.getActType() == PaohzDisAction.action_type_dis){
                	copy.setActType(PaohzDisAction.action_type_autoplaydis);
                }
            }else if(copy.getSeat()==lastWinSeat&&player.getSeat()==shuXingSeat){
                copy.setHuxi(winPlayer.getOutHuxi() + winPlayer.getZaiHuxi());
                if(winPlayer.isAutoPlay() && copy.getActType() == PaohzDisAction.action_type_dis){
                    copy.setActType(PaohzDisAction.action_type_autoplaydis);
                }
            }

			// ???????????????????????????
            if (copy.getAction() == PaohzDisAction.action_zai) {
                if (copy.getSeat() != player.getSeat()) {
                    if (copy.getSeat()==lastWinSeat&&player.getSeat()==shuXingSeat){
                    }else{
						// ???????????????0
                        List<Integer> ids = PaohuziTool.toPhzCardZeroIds(copy.getPhzIdsList());
                        copy.clearPhzIds();
                        copy.addAllPhzIds(ids);
                    }
                }
            }

            if (actionSeatMap.containsKey(player.getSeat())) {
                List<Integer> actionList = getSendSelfAction(zaiKeyValue, player.getSeat(), actionSeatMap.get(player.getSeat()));
                if (actionList != null) {
                    // copy.addAllSelfAct(actionList);
                    if (noShow && paoSeat != player.getSeat()) {
						// ????????????????????????????????????????????????
                        if (1 == actionList.get(0)) {
                            copy.addAllSelfAct(actionList);
                        }
                    } else {
                        copy.addAllSelfAct(actionList);
                    }
                }

            }else if (player.getSeat()==shuXingSeat&&actionSeatMap.containsKey(winPlayer.getSeat())) {
                List<Integer> actionList = getSendSelfAction(zaiKeyValue, winPlayer.getSeat(), actionSeatMap.get(winPlayer.getSeat()));
                if (actionList != null) {
                    // copy.addAllSelfAct(actionList);
                    if (noShow && paoSeat != winPlayer.getSeat()) {
						// ????????????????????????????????????????????????
                        if (1 == actionList.get(0)) {
                            copy.addAllSelfAct(actionList);
                        }
                    } else {
                        copy.addAllSelfAct(actionList);
                    }
                }

            }

            player.writeSocket(copy.build());

            if (copy.getSelfActList() != null && copy.getSelfActList().size() > 0) {
                StringBuilder sb = new StringBuilder("SyPhz");
                sb.append("|").append(getId());
                sb.append("|").append(getPlayBureau());
                sb.append("|").append(player.getUserId());
                sb.append("|").append(player.getSeat());
                sb.append("|").append(player.isAutoPlay() ? 1 : 0);
                sb.append("|").append("actList");
                sb.append("|").append(PaohuziCheckCardBean.actionListToString(actionSeatMap.get(player.getSeat())));
                LogUtil.msgLog.info(sb.toString());
            }
        }
    }

    /**
	 * ??????????????????????????????
	 */
    private void checkSendActionMsg() {
        if (actionSeatMap.isEmpty()) {
            return;
        }

        PlayPaohuziRes.Builder disBuilder = PlayPaohuziRes.newBuilder();
        SyPaohuziPlayer disCSMajiangPlayer = seatMap.get(disCardSeat);
        PaohuziResTool.buildPlayRes(disBuilder, disCSMajiangPlayer, 0, null);
        disBuilder.setRemain(leftCards.size());
        disBuilder.setHuxi(disCSMajiangPlayer.getOutHuxi() + disCSMajiangPlayer.getZaiHuxi());
        // disBuilder.setNextSeat(nowDisCardSeat);
        setNextSeatMsg(disBuilder);
        for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()) {
            PlayPaohuziRes.Builder copy = disBuilder.clone();
            List<Integer> actionList = entry.getValue();
            copy.addAllSelfAct(actionList);
            SyPaohuziPlayer seatPlayer = seatMap.get(entry.getKey());
            seatPlayer.writeSocket(copy.build());
            if (shuXingSeat>0&&seatPlayer.getSeat()==lastWinSeat){
                seatMap.get(shuXingSeat).writeSocket(copy.build());
            }
        }

    }

    public void checkAction() {
        int nowSeat = getNowDisCardSeat();
		// ????????????????????????
        SyPaohuziPlayer nowPlayer = seatMap.get(nowSeat);
        if (nowPlayer == null) {
            return;
        }
        PaohuziCheckCardBean checkCard = nowPlayer.checkCard(null, true, true, false);
        if (checkPaohuziCheckCard(checkCard)) {
            playAutoDisCard(checkCard);
            tiLong(nowPlayer);
			/*-- ??????????????????????????????
			boolean needBuPai = false;
			if (checkCard.isTi()) {
				PaohuziHandCard cardBean = nowPlayer.getPaohuziHandCard();
				PaohzCardIndexArr valArr = cardBean.getIndexArr();
				PaohuziIndex index3 = valArr.getPaohzCardIndex(3);
				if (index3 != null && index3.getLength() >= 2) {
					needBuPai = true;
				}
			}
			playAutoDisCard(checkCard);
			
			if (needBuPai) {
				PaohzCard buPai = leftCards.remove(0);
				for (SyPaohuziPlayer player : seatMap.values()) {
					if (nowSeat == player.getSeat()) {
						player.getHandPhzs().add(buPai);
						System.out.println("----------------------------------??????:" + player.getName() + "  ?????????:" + buPai.getId() + "  ????????????:" + leftCards.size());
					}
					player.writeComMessage(WebSocketMsgType.res_com_code_phzbupai, nowSeat, buPai.getId(), leftCards.size());
				}
			}
			 */
        }
        checkSendActionMsg();
    }

    /**
	 * ????????????
	 */
    private void playAutoDisCard(PaohuziCheckCardBean checkCard) {
        playAutoDisCard(checkCard, false);
    }

    /**
	 * ????????????
	 *
	 * @param moPai
	 *            ??????????????? ????????????????????????
	 */
    private void playAutoDisCard(PaohuziCheckCardBean checkCard, boolean moPai) {
        if (checkCard.getActionList() != null) {
            int seat = checkCard.getSeat();
            SyPaohuziPlayer player = seatMap.get(seat);
            if (player.isRobot()) {
                sleep();
            }
            List<Integer> list = PaohuziTool.toPhzCardIds(checkCard.getAutoDisList());
            play(player, list, checkCard.getAutoAction(), moPai, false, checkCard.isPassHu());

            if (actionSeatMap.isEmpty()) {
                setAutoDisBean(null);
            }
        }

    }

    private void sleep() {
        try {
            Thread.sleep(1500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void robotDealAction() {
        if (isTest()) {
            if (leftCards.size() == 0 && !isHasSpecialAction()) {
                calcOver();
                return;
            }
            if (actionSeatMap.isEmpty()) {
                int nextseat = getNowDisCardSeat();
                SyPaohuziPlayer player = seatMap.get(nextseat);
                if (player != null && player.isRobot()) {
					// ????????????
                    PaohuziHandCard paohuziHandCardBean = player.getPaohuziHandCard();
                    int card = RobotAI.getInstance().outPaiHandle(0, PaohuziTool.toPhzCardIds(paohuziHandCardBean.getOperateCards()), new ArrayList<Integer>());
                    if (card == 0) {
                        return;
                    }
                    sleep();
                    List<Integer> cardList = new ArrayList<>(Arrays.asList(card));
                    play(player, cardList, 0);
                }
            } else {
                // (Entry<Integer, List<Integer>> entry :
                // actionSeatMap.entrySet())
                Iterator<Integer> iterator = actionSeatMap.keySet().iterator();
                while (iterator.hasNext()) {
                    Integer key = iterator.next();
                    List<Integer> value = actionSeatMap.get(key);
                    SyPaohuziPlayer player = seatMap.get(key);
                    if (player == null || !player.isRobot()) {
						// player.writeErrMsg(player.getName() + " ?????????" +
                        // entry.getValue());
                        continue;
                    }
                    List<Integer> actions = PaohzDisAction.parseToDisActionList(value);
                    for (int action : actions) {
                        if (!checkAction(player, action,null,null)) {
                            continue;
                        }
                        sleep();
                        if (action == PaohzDisAction.action_hu) {
							broadMsg(player.getName() + "??????");
                            play(player, null, action);
                        } else if (action == PaohzDisAction.action_peng) {
                            play(player, null, action);

                        } else if (action == PaohzDisAction.action_chi) {
                            play(player, null, action);

                        } else if (action == PaohzDisAction.action_pao) {
                            // play(player, null, action);
                        } else if (action == PaohzDisAction.action_ti) {
                            // play(player,
                            // PaohuziTool.toPhzCardIds(nowDisCardIds), action);
                        }

                        break;

                    }
                }
            }

        }
    }

    @Override
    public int getPlayerCount() {
        return seatMap.size();
    }

    @Override
    protected void initNext1() {
        setSendPaoSeat(0);
        setZaiCard(null);
        setBeRemoveCard(null);
        setAutoDisBean(null);
        clearMarkMoSeat();
        clearMoSeatPair();
        clearHuList();
        setLeftCards(null);
        setStartLeftCards(null);
        setMoFlag(0);
        setMoSeat(0);
        clearAction();
        setNowDisCardSeat(0);
        setNowDisCardIds(null);
        setFirstCard(true);
        if (isSiRenBoPi()) {
            setShuXingSeat(calcNextNextSeat(getLastWinSeat()));
        }
        timeNum = 0 ;
        clearTempAction();
        finishFapai=0;
        setPaoHu(0);
    }

    @Override
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
                tempMap.put("outPai1", seatMap.get(1).buildOutPaiStr());
            }
            if (tempMap.containsKey("outPai2")) {
                tempMap.put("outPai2", seatMap.get(2).buildOutPaiStr());
            }
            if (tempMap.containsKey("outPai3")) {
                tempMap.put("outPai3", seatMap.get(3).buildOutPaiStr());
            }
            if (tempMap.containsKey("outPai4")) {
                tempMap.put("outPai4", seatMap.get(4).buildOutPaiStr());
            }
            if (tempMap.containsKey("handPai1")) {
                tempMap.put("handPai1", seatMap.get(1).buildHandPaiStr());
            }
            if (tempMap.containsKey("handPai2")) {
                tempMap.put("handPai2", seatMap.get(2).buildHandPaiStr());
            }
            if (tempMap.containsKey("handPai3")) {
                tempMap.put("handPai3", seatMap.get(3).buildHandPaiStr());
            }
            if (tempMap.containsKey("handPai4")) {
                tempMap.put("handPai4", seatMap.get(4).buildHandPaiStr());
            }
            if (tempMap.containsKey("answerDiss")) {
                tempMap.put("answerDiss", buildDissInfo());
            }
            if (tempMap.containsKey("nowDisCardIds")) {
                tempMap.put("nowDisCardIds", StringUtil.implode(PaohuziTool.toPhzCardIds(nowDisCardIds), ","));
            }
            if (tempMap.containsKey("leftPais")) {
                tempMap.put("leftPais", StringUtil.implode(PaohuziTool.toPhzCardIds(leftCards), ","));
            }
            if (tempMap.containsKey("nowAction")) {
                tempMap.put("nowAction", buildNowAction());
            }
            if (tempMap.containsKey("extend")) {
                tempMap.put("extend", buildExtend());
            }
            //            TableDao.getInstance().save(tempMap);
        }
        return tempMap.size() > 0 ? tempMap : null;
    }

    @Override
    public JsonWrapper buildExtend0(JsonWrapper wrapper) {
//		JsonWrapper wrapper = new JsonWrapper("");
        wrapper.putString(1, StringUtil.implode(huConfirmList, ","));
        wrapper.putInt(2, moFlag);
        wrapper.putInt(3, toPlayCardFlag);
        wrapper.putInt(4, moSeat);
        if (moSeatPair != null) {
            String moSeatPairVal = moSeatPair.getId() + "_" + moSeatPair.getValue();
            wrapper.putString(5, moSeatPairVal);
        }
        if (autoDisBean != null) {
            wrapper.putString(6, autoDisBean.buildAutoDisStr());

        } else {
            wrapper.putString(6, "");
        }
        if (zaiCard != null) {
            wrapper.putInt(7, zaiCard.getId());
        }
        wrapper.putInt(8, sendPaoSeat);
        wrapper.putInt(9, firstCard ? 1 : 0);
        if (beRemoveCard != null) {
            wrapper.putInt(10, beRemoveCard.getId());
        }
        wrapper.putInt(11, shuXingSeat);
        wrapper.putInt(12, maxPlayerCount);
        wrapper.putString("startLeftCards", startLeftCardsToJSON());
        wrapper.putInt(13, ceiling);
		wrapper.putInt(14, isRedBlack);
        wrapper.putInt(15, isLianBanker);
        wrapper.putInt(16, xiTotun);
        wrapper.putInt("catCardCount", catCardCount);

        wrapper.putInt(17, jiaBei);
        wrapper.putInt(18, jiaBeiFen);
        wrapper.putInt(19, jiaBeiShu);
        wrapper.putInt(20, autoPlayGlob);
        wrapper.putInt(21, autoTimeOut);
        JSONArray tempJsonArray = new JSONArray();
        for (int seat : tempActionMap.keySet()) {
            tempJsonArray.add(tempActionMap.get(seat).buildData());
        }
        wrapper.putString("22", tempJsonArray.toString());
        wrapper.putInt(23, chui);
        wrapper.putInt(24, finishFapai);
        wrapper.putInt(25, below);
        wrapper.putInt(26, belowAdd);
        wrapper.putInt(27, paoHu);
        return wrapper;
    }

    private String startLeftCardsToJSON() {
        JSONArray jsonArray = new JSONArray();
        for (int card : startLeftCards) {
            jsonArray.add(card);
        }
        return jsonArray.toString();
    }

    @Override
    public void fapai() {
        synchronized (this){
            if (maxPlayerCount<=1||maxPlayerCount>4){
                return;
            }

            changeTableState(table_state.play);
            deal();
        }
    }

    @Override
    protected void deal() {
        for (SyPaohuziPlayer player : playerMap.values()) {
            if(!player.isAlreadyMo())
                player.setLastCheckTime(0);
        }
        if(getPlayBureau()==1 && isGroupRoom()){
            setLastWinSeat(new Random().nextInt(getMaxPlayerCount())+1);
        }
        if (lastWinSeat == 0) {
            int masterseat = playerMap.get(masterId).getSeat();
            setLastWinSeat(masterseat);
        }
        if (isSiRenBoPi()) {
            setShuXingSeat(calcNextNextSeat(lastWinSeat));// ????????????????????????
        }
        setDisCardSeat(lastWinSeat);
        setNowDisCardSeat(lastWinSeat);
        setMoSeat(lastWinSeat);
        setToPlayCardFlag(1);
        markMoSeat(null, lastWinSeat);
        List<Integer> copy = new ArrayList<>(PaohuziConstant.cardList);
        List<List<PaohzCard>> list = PaohuziTool.fapai(copy, zp,getMaxPlayerCount());
        int i = 1;
        for (SyPaohuziPlayer player : playerMap.values()) {
            player.changeState(player_state.play);
            player.getFirstPais().clear();
            if (player.getSeat() == lastWinSeat) {
                player.dealHandPais(list.get(0));
                player.getFirstPais().addAll(PaohuziTool.toPhzCardIds(new ArrayList(list.get(0))));// ?????????????????????????????????????????????

                StringBuilder sb = new StringBuilder("SyPhz");
                sb.append("|").append(getId());
                sb.append("|").append(getPlayBureau());
                sb.append("|").append(player.getUserId());
                sb.append("|").append(player.getSeat());
                sb.append("|").append(player.getName());
                sb.append("|").append("fapai");
                sb.append("|").append(player.getHandPais());
                sb.append("|").append(player.getHandPhzs());
                LogUtil.msgLog.info(sb.toString());
                continue;
            }
            // ???????????????,????????????List
            if (player.getSeat() == shuXingSeat) {
                player.dealHandPais(new ArrayList<PaohzCard>());
                continue;
            }
            player.dealHandPais(list.get(i));
            player.getFirstPais().addAll(PaohuziTool.toPhzCardIds(new ArrayList(list.get(i))));// ?????????????????????????????????????????????
            i++;

            StringBuilder sb = new StringBuilder("SyPhz");
            sb.append("|").append(getId());
            sb.append("|").append(getPlayBureau());
            sb.append("|").append(player.getUserId());
            sb.append("|").append(player.getSeat());
            sb.append("|").append(player.getName());
            sb.append("|").append("fapai");
            sb.append("|").append(player.getHandPais());
            sb.append("|").append(player.getHandPhzs());
            LogUtil.msgLog.info(sb.toString());
        }

        List<PaohzCard> cardList = new ArrayList<>(list.get(3));
        if (maxPlayerCount<=2){
            cardList.addAll(list.get(2));
        }
        int size = cardList.size();
        // ?????????????????????
        setStartLeftCards(PaohuziTool.toPhzCardIds(cardList));

        // ??????????????????
        if (catCardCount<=0){
            setLeftCards(cardList);
        }else if (catCardCount>=cardList.size()){
            setLeftCards(null);
        }else{
            setLeftCards(new ArrayList<>(cardList.subList(catCardCount,cardList.size())));
        }
        finishFapai=1;
    }

    @Override
    public int getNextDisCardSeat() {
        if (disCardSeat == 0) {
            return lastWinSeat;
        }
        return calcNextSeat(disCardSeat);
    }

    /**
	 * ??????seat???????????????
	 */
    public int calcNextSeat(int seat) {
        int nextSeat = seat + 1 > maxPlayerCount ? 1 : seat + 1;
        if (nextSeat == shuXingSeat) {
            nextSeat = nextSeat + 1 > maxPlayerCount ? 1 : nextSeat + 1;
        }
        return nextSeat;
    }

    /**
	 * ??????seat???????????????
	 */
    public int calcFrontSeat(int seat) {
        int frontSeat = seat - 1 < 1 ? maxPlayerCount : seat - 1;
        if (frontSeat == shuXingSeat) {
            frontSeat = frontSeat - 1 < 1 ? maxPlayerCount : frontSeat - 1;
        }
        return frontSeat;
    }

    /**
	 * ??????????????????
	 */
    public int calcNextNextSeat(int seat) {
        int nextSeat = seat + 1 > maxPlayerCount ? 1 : seat + 1;
        int nextNextSeat = nextSeat + 1 > maxPlayerCount ? 1 : nextSeat + 1;
        return nextNextSeat;
    }

    @Override
    public Player getPlayerBySeat(int seat) {
        return seatMap.get(seat);
    }

    @Override
    public Map<Integer, Player> getSeatMap() {
        Object o = seatMap;
        return (Map<Integer, Player>) o;
    }

    @Override
    public Map<Long, Player> getPlayerMap() {
        Object o = playerMap;
        return (Map<Long, Player>) o;
    }

    @Override
    public CreateTableRes buildCreateTableRes(long userId, boolean isrecover, boolean isLastReady) {
        CreateTableRes.Builder res = CreateTableRes.newBuilder();
        buildCreateTableRes0(res);
        res.setNowBurCount(getPlayBureau());
        res.setTotalBurCount(getTotalBureau());
        res.setGotyeRoomId(gotyeRoomId + "");
        res.setTableId(getId() + "");
        res.setWanfa(playType);
        res.setRenshu(maxPlayerCount);
        if (leftCards != null) {
            res.setRemain(leftCards.size());
        } else {
            res.setRemain(0);
        }

        KeyValuePair<Boolean, Integer> zaiKeyValue = getZaiOrTiKeyValue();
        int autoCheckTime = 0;
        List<PlayerInTableRes> players = new ArrayList<>();
        for (SyPaohuziPlayer player : playerMap.values()) {
            PlayerInTableRes.Builder playerRes = player.buildPlayInTableInfo(userId, isrecover);
            if (playerRes==null){
                continue;
            }
			// ????????????
            playerRes.addRecover((player.getSeat() == lastWinSeat) ? 1 : 0);
            if (player.getUserId() == userId) {
                if (player.getSeat()==shuXingSeat){
                    SyPaohuziPlayer winPlayer = seatMap.get(lastWinSeat);

                    playerRes.addAllHandCardIds(winPlayer.getHandPais());
                    if (actionSeatMap.containsKey(winPlayer.getSeat())) {
                        List<Integer> actionList = getSendSelfAction(zaiKeyValue, winPlayer.getSeat(), actionSeatMap.get(winPlayer.getSeat()));
                        if (actionList != null) {
                            playerRes.addAllRecover(actionList);
                        }
                    }
                }else{
                    playerRes.addAllHandCardIds(player.getHandPais());
                    if (actionSeatMap.containsKey(player.getSeat())) {
                        List<Integer> actionList = getSendSelfAction(zaiKeyValue, player.getSeat(), actionSeatMap.get(player.getSeat()));
                        if (actionList != null && !tempActionMap.containsKey(player.getSeat()) && !huConfirmList.contains(player.getSeat())) {
                            playerRes.addAllRecover(actionList);
                        }
                    }
                }
            }
            players.add(playerRes.build());

            if (autoPlay && player.isCheckAuto()) {
                int timeOut = autoTimeOut;
                if (player.getAutoPlayCheckedTime() >= autoTimeOut && !player.isAutoPlayCheckedTimeAdded()) {
                    timeOut = autoTimeOut2;
                }
                autoCheckTime = timeOut - (int) (System.currentTimeMillis() - player.getLastCheckTime());
            }
        }
        res.addAllPlayers(players);
        if (actionSeatMap.isEmpty()) {
            // int nextSeat = getNextDisCardSeat();
            if (nowDisCardSeat != 0) {
                if (toPlayCardFlag == 1) {
                    res.setNextSeat(nowDisCardSeat);
                } else {
                    res.setNextSeat(0);
                }
            }
        }
        res.addExt(nowDisCardSeat); // 0
        res.addExt(payType);// 1
		// ?????????
        res.addExt(ceiling);// 2
		res.addExt(isRedBlack);// 3
        res.addExt(isLianBanker);// 4
        res.addExt(xiTotun);// 5
        res.addExt(modeId.length()>0?Integer.parseInt(modeId):0);//6
		int ratio;
		int pay;
		if (isGoldRoom()){
            ratio = 0;
            pay = 0;
		}else{
			ratio = 1;
			pay = consumeCards()?loadPayConfig(payType):0;
		}
		res.addExt(ratio);// 7
		res.addExt(pay);// 8
        res.addExt(catCardCount);// 9

        res.addExt(creditMode);     // 10
//        res.addExt(creditJoinLimit);// 11
//        res.addExt(creditDissLimit);// 12
//        res.addExt(creditDifen);    // 13
//        res.addExt(creditCommission);// 14

        res.addExt(0);
        res.addExt(0);
        res.addExt(0);
        res.addExt(0);
        res.addExt(creditCommissionMode1);// 15
        res.addExt(creditCommissionMode2);// 16
        res.addExt(autoPlay ? 1 : 0);// 17
        res.addExt(jiaBei);// 18
        res.addExt(jiaBeiFen);// 19
        res.addExt(jiaBeiShu);// 20

        res.addTimeOut((isGoldRoom() || autoPlay) ?(int)autoTimeOut:0);
        res.addTimeOut(autoCheckTime);
        res.addTimeOut((isGoldRoom() || autoPlay) ?(int) autoTimeOut2 :0);
        return res.build();
    }

    @Override
    public void setConfig(int index, int val) {

    }

    public int randNumber(int number) {
        if (isGoldRoom()) {
            return number;
        } else {
            int ret = 0;
            if (number > 0) {
                ret = (number + 5) / 10 * 10;
            } else if (number < 0) {
                ret = (number - 5) / 10 * 10;
            }
            return ret;
        }
    }

    public int getBopiPoint(SyPaohuziPlayer player) {
        if (!isBoPi()) {
            return 0;
        }

        int selfPoint = 0;
        int otherPoint = 0;
        int retPoint = 0;
        for (SyPaohuziPlayer temp : seatMap.values()) {
            if (player.getUserId() == temp.getUserId()) {
                selfPoint = randNumber(temp.getTotalPoint());
            } else {
                otherPoint += randNumber(temp.getTotalPoint());
            }
        }

        retPoint = selfPoint * (seatMap.size() - 1) - otherPoint;
        return retPoint;
    }

    public ClosingPhzInfoRes.Builder sendAccountsMsg(boolean over, List<Integer> winList, int winFen, List<Integer> mt, int totalTun, boolean isBreak,Map<Long,Integer> outScoreMap,Map<Long,Integer> ticketMap) {
        List<ClosingPhzPlayerInfoRes> list = new ArrayList<>();
        List<ClosingPhzPlayerInfoRes.Builder> builderList = new ArrayList<>();
        SyPaohuziPlayer winPlayer = null;
        boolean isBoPi = isBoPi();

        List<TablePhzResMsg.PhzHuCardList> cardCombos = new ArrayList<>();
        for (SyPaohuziPlayer player : seatMap.values()) {
            if (winList != null && winList.contains(player.getSeat())) {
                winPlayer = seatMap.get(player.getSeat());
            }
            ClosingPhzPlayerInfoRes.Builder build;
            if (over) {
                build = player.bulidTotalClosingPlayerInfoRes(isBoPi, true, player.getWinLossPoint());
            } else {
                build = player.bulidOneClosingPlayerInfoRes(isBoPi, false, player.getWinLossPoint());
            }
            if (isSiRenBoPi()) {
                build.setIsShuXing(shuXingSeat);
            }

			build.addAllFirstCards(player.getFirstPais());// ?????????????????????????????????
            for(int action : player.getActionTotalArr()){
            	build.addStrExt(action+"");
    		}
            if (isGoldRoom()){
                build.setTotalPoint((int)player.getWinGold());
                build.setPoint((int)player.getWinGold());
				build.addStrExt("1");//4
				build.addStrExt(player.loadAllGolds()<=0?"1":"0");//5
				build.addStrExt(outScoreMap==null?"0":outScoreMap.getOrDefault(player.getUserId(),0).toString());//6
			}else{
				build.addStrExt("0");
				build.addStrExt("0");
				build.addStrExt("0");
			}
            build.addStrExt(ticketMap==null?"0":String.valueOf(ticketMap.getOrDefault(player.getUserId(),0)));//7
            builderList.add(build);

			// ?????????
            if(isCreditTable()){
                if(isBoPi){
                    player.setWinLoseCredit(player.getWinLossPoint() * creditDifen);
                }else{
                    player.setWinLoseCredit(player.getTotalPoint() * creditDifen);
                }
            }
            TablePhzResMsg.PhzHuCardList.Builder builder = TablePhzResMsg.PhzHuCardList.newBuilder();
            builder.setSeat(player.getSeat());
            builder.addAllPhzCard(player.buildNormalPhzHuCards());
            cardCombos.add(builder.build());
        }

		// ???????????????
        if (isCreditTable()) {
			// ??????????????????
            calcNegativeCredit();

            long dyjCredit = 0;
            for (SyPaohuziPlayer player : seatMap.values()) {
                if (player.getWinLoseCredit() > dyjCredit) {
                    dyjCredit = player.getWinLoseCredit();
                }
            }
            for (ClosingPhzPlayerInfoRes.Builder builder : builderList) {
                SyPaohuziPlayer player = seatMap.get(builder.getSeat());
                calcCommissionCredit(player, dyjCredit);

                builder.addStrExt(player.getWinLoseCredit() + "");      //8
                builder.addStrExt(player.getCommissionCredit() + "");   //9
				// 2019-02-26??????
                builder.setWinLoseCredit(player.getWinLoseCredit());
                builder.setCommissionCredit(player.getCommissionCredit());
            }
        } else if (isGroupTableGoldRoom()) {
            // -----------??????????????????---------------------------------
            for (SyPaohuziPlayer player : seatMap.values()) {
                if(isBoPi){
                    player.setWinGold(player.getWinLossPoint() * gtgDifen);
                }else{
                    player.setWinGold(player.getTotalPoint() * gtgDifen);
                }
            }
            calcGroupTableGoldRoomWinLimit();
            for (ClosingPhzPlayerInfoRes.Builder builder : builderList) {
                SyPaohuziPlayer player = seatMap.get(builder.getSeat());

                builder.addStrExt(player.getWinLoseCredit() + "");      //8
                builder.addStrExt(player.getCommissionCredit() + "");   //9

                builder.setWinLoseCredit(player.getWinGold());
            }
        } else {
            for (ClosingPhzPlayerInfoRes.Builder builder : builderList) {
                builder.addStrExt(0 + ""); //8
                builder.addStrExt(0 + ""); //9
            }
        }
        for (ClosingPhzPlayerInfoRes.Builder builder : builderList) {
            SyPaohuziPlayer player = seatMap.get(builder.getSeat());
            builder.addStrExt(player.getChui() + "");      //10
            list.add(builder.build());
        }

        ClosingPhzInfoRes.Builder res = ClosingPhzInfoRes.newBuilder();
        res.addAllLeftCards(PaohuziTool.toPhzCardIds(leftCards));
        if (mt != null) {
            res.addAllFanTypes(mt);
        }
        if (winPlayer != null) {
			res.setTun(isBoPi() ? 0 : totalTun);// ?????????0???
            res.setFan(winFen);
            res.setHuxi(winPlayer.getTotalHu());
            res.setTotalTun(totalTun);
            res.setHuSeat(winPlayer.getSeat());
            if (winPlayer.getHu() != null && winPlayer.getHu().getCheckCard() != null) {
                res.setHuCard(winPlayer.getHu().getCheckCard().getId());
            }
            res.addAllCards(winPlayer.buildPhzHuCards());
        }
        res.addAllClosingPlayers(list);
        res.setIsBreak(isBreak ? 1 : 0);
        res.setWanfa(getWanFa());
        res.addAllExt(buildAccountsExt(over));
        res.addAllStartLeftCards(startLeftCards);
        res.addAllAllCardsCombo(cardCombos);

        if (over && isGroupRoom() && !isCreditTable()) {
            res.setGroupLogId((int) saveUserGroupPlaylog());
        }
        for (SyPaohuziPlayer player : seatMap.values()) {
            player.writeSocket(res.build());
        }
        return res;
    }

    private void countBetweenTwoPoint(SyPaohuziPlayer p1,SyPaohuziPlayer p2){
        if(p1 == null || p2 == null){
            return;
        }
        int self = randNumber(p1.getTotalPoint());
        int other = randNumber(p2.getTotalPoint());
        int point =self-other;
        if(point==0)
            return;
        if(chui==1){
            point*=Math.pow(2,p1.getChui()+p2.getChui());
        }
        p1.changeWinLossPoint(point);
        p2.changeWinLossPoint(-point);
    }

    @Override
    public void sendAccountsMsg() {
        calcPointBeforeOver();
        ClosingPhzInfoRes.Builder res = sendAccountsMsg(true, null, 0, null, 0, true, null,null);
        saveLog(true,0L, res.build());
    }

    public List<String> buildAccountsExt(boolean isOver) {
        List<String> ext = new ArrayList<>();
        ext.add(id + "");
        ext.add(masterId + "");
        ext.add(TimeUtil.formatTime(TimeUtil.now()));
        ext.add(playType + "");
        ext.add(getConifg(0) + "");
        ext.add(playBureau + "");
        ext.add(isOver ? 1 + "" : 0 + "");
        ext.add(maxPlayerCount + "");
        ext.add(isGroupRoom() ? "1" : "0");
		ext.add(isOver ? dissInfo() : "");
		// ???????????????0
		ext.add(modeId);
		int ratio;
		int pay;
		if (isGoldRoom()){
            ratio = 0;
            pay = 0;
		}else{
			ratio = 1;
			pay = loadPayConfig(payType);
		}
		ext.add(String.valueOf(ratio));
		ext.add(String.valueOf(pay>=0?pay:0));
        ext.add(isGroupRoom()?loadGroupId():"");//13
        ext.add(String.valueOf(catCardCount));//14


		// ?????????
        ext.add(creditMode + ""); //15
        ext.add(creditJoinLimit + "");//16
        ext.add(creditDissLimit + "");//17
        ext.add(creditDifen + "");//18
        ext.add(creditCommission + "");//19
        ext.add(creditCommissionMode1 + "");//20
        ext.add(creditCommissionMode2 + "");//21
        ext.add(autoPlay ? "1" : "0");//20
        ext.add(jiaBei + "");//22
        ext.add(jiaBeiFen + "");//23
        ext.add(jiaBeiShu + "");//24
        return ext;
    }
	private String dissInfo(){
    	JSONObject jsonObject = new JSONObject();
    	if(getSpecialDiss() == 1){
			jsonObject.put("dissState", "1");// ????????????
        }else{
            if(answerDissMap != null && !answerDissMap.isEmpty()){
				jsonObject.put("dissState", "2");// ??????????????????
                StringBuilder str = new StringBuilder();
                for(Entry<Integer, Integer> entry : answerDissMap.entrySet()){
                    Player player0 = getSeatMap().get(entry.getKey());
                    if(player0 != null){
                        str.append(player0.getUserId()).append(",");
                    }
                }
                if(str.length()>0){
                    str.deleteCharAt(str.length()-1);
                }
                jsonObject.put("dissPlayer", str.toString());
            }else{
				jsonObject.put("dissState", "0");// ????????????
            }
        }
    	return jsonObject.toString();
    }

    @Override
    public int getMaxPlayerCount() {
        return maxPlayerCount;
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
		info.setExtend(buildExtend());
		TableDao.getInstance().save(info);
		loadFromDB(info);
		return true;
	}

    public boolean createSimpleTable(Player player, int play, int bureauCount, List<Integer> params, List<String> strParams, boolean saveDb) throws Exception {
        return createTable(new CreateTableInfo(player, TABLE_TYPE_NORMAL, play, bureauCount, params, strParams, saveDb));
    }

    public void createTable(Player player, int play, int bureauCount, List<Integer> params) throws Exception {
        createTable(new CreateTableInfo(player, TABLE_TYPE_NORMAL, play, bureauCount, params, true));
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

        long id = getCreateTableId(player.getUserId(), play);
        if (id <= 0) {
            return false;
        }
        if(saveDb){
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
        }else{
        	setPlayType(play);
			setDaikaiTableId(daikaiTableId);
			this.id=id;
			this.totalBureau=bureauCount;
			this.playBureau=1;
        }
		int playerCount = StringUtil.getIntValue(params, 7, 0);// ????????????
		int payType = StringUtil.getIntValue(params, 9, 0);// ????????????
		int ceiling = StringUtil.getIntValue(params, 10, 0);// ????????????
		int isRedBlack = StringUtil.getIntValue(params, 11, 0);// ?????????
        if (play == PaohuziConstant.play_type_shaoyang){
        	isRedBlack = 1;
        }
		int isLianBanker = StringUtil.getIntValue(params, 12, 0);// ?????????
		int xiTotun = StringUtil.getIntValue(params, 13, 3);// ??????????????? 3???????????? 5????????????
		int catCardCount = StringUtil.getIntValue(params, 14, 0);// ??????????????????

        this.autoPlay = StringUtil.getIntValue(params, 23, 0) >= 1;

        this.jiaBei = StringUtil.getIntValue(params, 24, 0);
        this.jiaBeiFen = StringUtil.getIntValue(params, 25, 100);
        this.jiaBeiShu = StringUtil.getIntValue(params, 26, 1);
        
        autoPlayGlob = StringUtil.getIntValue(params, 27, 0);
        if(isBoPi()){
            chui = StringUtil.getIntValue(params, 28, 0);
        }
        setMaxPlayerCount(playerCount);
        if(maxPlayerCount==2){
            int belowAdd = StringUtil.getIntValue(params, 29, 0);
            if(belowAdd<=100&&belowAdd>=0)
                this.belowAdd=belowAdd;
            int below = StringUtil.getIntValue(params, 30, 0);
            if(below<=100&&below>=0){
                this.below=below;
                if(belowAdd>0&&below==0)
                    this.below=10;
            }
        }
        if (playerCount<=1||playerCount>4){
            return false;
        }
        if(playerCount == 3 || playerCount == 4){
            catCardCount = 0 ;
        }
        this.catCardCount = catCardCount;

        if(this.getMaxPlayerCount() != 2){
            jiaBei = 0 ;
        }
        setPayType(payType);
        if (PaohuziConstant.isPlayBopi(play)){
        	setCeiling(ceiling);
        }
        setIsRedBlack(isRedBlack);
        setIsLianBanker(isLianBanker);
        setXiTotun(xiTotun);

        if(autoPlay){
            int time = StringUtil.getIntValue(params, 23, 0);
            if(time ==1) {
                time=60;
            }
            autoTimeOut2 =autoTimeOut =time*1000 ;
        }
        changeExtend();
        LogUtil.msgLog.info("createTable tid:"+getId()+" "+player.getName() + " params"+params.toString());
        return true;
    }

    @Override
    public int getWanFa() {
        return SharedConstants.game_type_paohuzi;
    }

    @Override
    public boolean isTest() {
        return PaohuziConstant.isTest;
    }

    @Override
    public void checkReconnect(Player player) {
        checkMo();
        // PaohuziCheckCardBean checkCard = player.checkCard(card, true);
        sendChuiReconnect(player);
    }

    private void sendChuiReconnect(Player player){
        if(chui==0||maxPlayerCount!=getPlayerCount())
            return;
        int count=0;
        for(Map.Entry<Integer,SyPaohuziPlayer> entry:seatMap.entrySet()){
            player_state state = entry.getValue().getState();
            if(state==player_state.play||state==player_state.ready)
                count++;
        }
        if(count!=maxPlayerCount)
            return;

        SyPaohuziPlayer p=(SyPaohuziPlayer)player;
//        if(p.getChui()==-1){
//            p.writeComMessage(WebSocketMsgType.res_code_sybp_chui);
//        }
    }

    @Override
    public void checkAutoPlay() {
        synchronized (this){
            if (getSendDissTime() > 0) {
                for (SyPaohuziPlayer player : seatMap.values()) {
                    if (player.getLastCheckTime() > 0) {
                        player.setLastCheckTime(player.getLastCheckTime() + 1 * 1000);
                    }
                }
                return;
            }

            if (isAutoPlayOff()) {
                // ????????????
                for (int seat : seatMap.keySet()) {
                    SyPaohuziPlayer player = seatMap.get(seat);
                    player.setAutoPlay(false, this);
                    player.setLastOperateTime(System.currentTimeMillis());
                }
                return;
            }

            if (autoPlay && state == table_state.ready && playedBureau > 0) {
                ++timeNum;
                for (SyPaohuziPlayer player : seatMap.values()) {
					// ????????????????????????5???????????????
                    if (timeNum >= 5 && player.isAutoPlay()) {
                        autoReady(player);
                    } else if (timeNum >= 30) {
                        autoReady(player);
                    }
                }
                return;
            }

            int timeout;
            if(state != table_state.play){
                return;
            }else if(autoPlay){
                timeout = autoTimeOut;
            }else{
                return;
            }
            //timeout = 10*1000;
            long autoPlayTime = ResourcesConfigsUtil.loadIntegerValue("ServerConfig","autoPlayTimePhz",2*1000);
            long now = TimeUtil.currentTimeMillis();

			// ?????????????????????
            if (finishFapai == 0 && playedBureau == 0&&chui==1) {
                for (SyPaohuziPlayer player : seatMap.values()) {
                    if(player.getChui()>=0){
                        continue;
                    }
                    boolean auto = checkPlayerAuto(player, timeout);
                    if (auto)
                        chui(player, 0);
                }
                return;
            }


            if(!actionSeatMap.isEmpty()){
                int action = 0,seat = 0;
                for (Entry<Integer, List<Integer>> entry : actionSeatMap.entrySet()){
                    List<Integer> list = PaohzDisAction.parseToDisActionList(entry.getValue());
                    int minAction = Collections.min(list);
                    if(action == 0){
                        action = minAction;
                        seat = entry.getKey();
                    }else if(minAction < action){
                        action = minAction;
                        seat = entry.getKey();
                    }else if(minAction == action){
                        int nearSeat = getNearSeat(disCardSeat, Arrays.asList(seat, entry.getKey()));
                        seat = nearSeat;
                    }
                }
                if(action > 0 && seat > 0){
                    SyPaohuziPlayer player = seatMap.get(seat);
                    if (player==null){
                        LogUtil.errorLog.error("auto play error:tableId={},seat={} is null,seatMap={},playerMap={}",id,seat,seatMap.keySet(),playerMap.keySet());
                        return;
                    }

                    boolean auto = player.isAutoPlay();
                    if(!auto){
                        auto = checkPlayerAuto(player,timeout);
                    }
                    if(auto){
                        if (player.getAutoPlayTime() == 0L) {
                            player.setAutoPlayTime(now);
                        } else if (player.getAutoPlayTime() > 0L && now - player.getAutoPlayTime() >= autoPlayTime){
                            player.setAutoPlayTime(0L);
                            if(action == PaohzDisAction.action_chi){
                                action = PaohzDisAction.action_pass;
                            }
                            if(action == PaohzDisAction.action_pass || action == PaohzDisAction.action_peng || action == PaohzDisAction.action_hu){
                                play(player, new ArrayList<Integer>(), action);
                            }else{
                                checkMo();
                            }
                        }
                        return;
                    }
                    if(action == PaohzDisAction.action_pao && player.getLastCheckTime()>0){
                        checkMo();
                    }
                }
            }else{
                SyPaohuziPlayer player = seatMap.get(nowDisCardSeat);
                if (player == null) {
                    return;
                }
                if(toPlayCardFlag==1){
                    boolean auto = player.isAutoPlay();
                    if(!auto){
                        auto = checkPlayerAuto(player,timeout);
                    }
                    if(auto){
                        if (player.getAutoPlayTime() == 0L) {
                            player.setAutoPlayTime(now);
                        } else if (player.getAutoPlayTime() > 0L && now - player.getAutoPlayTime() >= autoPlayTime){
                            player.setAutoPlayTime(0L);
                            PaohzCard paohzCard = PaohuziTool.autoDisCard(player.getHandPhzs());
                            if(paohzCard != null){
                                play(player, Arrays.asList(paohzCard.getId()), 0);
                            }
                        }
                    }
                }else{
                    checkMo();
                }
            }
        }
    }

    public boolean checkPlayerAuto(SyPaohuziPlayer player ,int timeout){
        long now = TimeUtil.currentTimeMillis();
        boolean auto = false;
        if (player.isAutoPlayChecked() || (player.getAutoPlayCheckedTime() >= timeout && !player.isAutoPlayCheckedTimeAdded())) {
            player.setAutoPlayChecked(true);
            timeout = autoTimeOut2;
        }
        long lastCheckTime = player.getLastCheckTime();
        if (lastCheckTime > 0) {
            int checkedTime = (int) (now - lastCheckTime);
//            if (checkedTime > 10 * 1000) {
//                player.addAutoPlayCheckedTime(1 * 1000);
//                if (!player.isAutoPlayCheckedTimeAdded()) {
//                    player.setAutoPlayCheckedTimeAdded(true);
//                    player.addAutoPlayCheckedTime(10 * 1000);
//                }
//                if(!player.isAutoPlayChecked() && player.getAutoPlayCheckedTime() >= timeout){
			// // ????????????
//                    ComMsg.ComRes msg = SendMsgUtil.buildComRes(133, player.getSeat(), (int) player.getUserId()).build();
//                    broadMsg(msg);
//                    broadMsg0(msg);
//                    auto = true;
//
//                }
//            }
            if (checkedTime >= timeout) {
                auto = true;
            }
            if(auto){
                player.setAutoPlay(true, this);
            }
//            System.out.println("checkPlayerAuto----" + player.getSeat() + "|" + player.getUserId() + "|" + player.getAutoPlayCheckedTime() + "|" + checkedTime + "|" + auto);
        } else {
            player.setLastCheckTime(now);
            player.setCheckAuto(true);
            player.setAutoPlayCheckedTimeAdded(false);
        }

        return auto;
    }

    @Override
    public Class<? extends Player> getPlayerClass() {
        return SyPaohuziPlayer.class;
    }

    public PaohzCard getNextCard(int val) {
        if (this.leftCards.size() > 0) {
            Iterator<PaohzCard> iterator = this.leftCards.iterator();
            PaohzCard find = null;
            while (iterator.hasNext()) {
                PaohzCard paohzCard = iterator.next();
                if (paohzCard.getVal() == val) {
                    find = paohzCard;
                    iterator.remove();
                    break;
                }
            }
            dbParamMap.put("leftPais", JSON_TAG);
            return find;
        }
        return null;
    }

    public PaohzCard getNextCard() {
        if (this.leftCards.size() > 0) {
            PaohzCard card = this.leftCards.remove(0);
            dbParamMap.put("leftPais", JSON_TAG);
            return card;
        }
        return null;
    }

    public List<PaohzCard> getLeftCards() {
        return leftCards;
    }

    public void setLeftCards(List<PaohzCard> leftCards) {
        if (leftCards == null) {
            this.leftCards.clear();
        } else {
            this.leftCards = leftCards;

        }
        dbParamMap.put("leftPais", JSON_TAG);
    }

    public void setStartLeftCards(List<Integer> startLeftCards) {
        if (startLeftCards == null) {
            this.startLeftCards.clear();
        } else {
            this.startLeftCards = startLeftCards;

        }
        changeExtend();
    }

    public int getMoSeat() {
        return moSeat;
    }

    public void setMoSeat(int lastMoSeat) {
        this.moSeat = lastMoSeat;
        changeExtend();
    }

    public List<PaohzCard> getNowDisCardIds() {
        return nowDisCardIds;
    }

    public void setNowDisCardIds(List<PaohzCard> nowDisCardIds) {
        this.nowDisCardIds = nowDisCardIds;
        dbParamMap.put("nowDisCardIds", JSON_TAG);
    }

    /**
	 * ???????????????????????????
	 */
    public boolean isMoFlag() {
        return moFlag == 1;
    }

    public void setMoFlag(int moFlag) {
        if (this.moFlag != moFlag) {
            this.moFlag = moFlag;
            changeExtend();
        }
    }

    public void markMoSeat(int seat, int action) {
        checkMoMark = new KeyValuePair<>();
        checkMoMark.setId(seat);
        checkMoMark.setValue(action);
        changeExtend();
    }

    private void clearMarkMoSeat() {
        checkMoMark = null;
        changeExtend();
    }

    public void markMoSeat(PaohzCard card, int seat) {
        moSeatPair = new KeyValuePair<>();
        if (card != null) {
            moSeatPair.setId(card.getId());
        }
        moSeatPair.setValue(seat);
        changeExtend();
    }

    public void clearMoSeatPair() {
        moSeatPair = null;
    }

    // public boolean checkMo

    public int getToPlayCardFlag() {
        return toPlayCardFlag;
    }

    public void setToPlayCardFlag(int toPlayCardFlag) {
        if (this.toPlayCardFlag != toPlayCardFlag) {
            this.toPlayCardFlag = toPlayCardFlag;
            changeExtend();
        }

    }

    @Override
    public boolean consumeCards() {
        return SharedConstants.consumecards;
    }

    public PaohzCard getZaiCard() {
        return zaiCard;
    }

    public void setZaiCard(PaohzCard zaiCard) {
        this.zaiCard = zaiCard;
        changeExtend();
    }

    public int getSendPaoSeat() {
        return sendPaoSeat;
    }

    public void setSendPaoSeat(int sendPaoSeat) {
        if (this.sendPaoSeat != sendPaoSeat) {
            this.sendPaoSeat = sendPaoSeat;
            changeExtend();
        }

    }


//	public int calcNeedRoomCards(int needCard, int playerCount) {
//		long endTime = 1482940800000L;// 2016-12-29 00:00:00
//		long nowTime = TimeUtil.currentTimeMillis();
//		if (nowTime < endTime) {
//			return 0;
//		}
//		if (isSiRenBoPi()) {
//			needCard = 2;
//		} else if (isBoPi()) {
//			needCard = 1;
//		}
//		super.calcNeedRoomCards(needCard, playerCount);
//		return needCard;
//	}

    public boolean isBoPi() {
        return playType == PaohuziConstant.play_type_bopi;
    }

    public Map<Integer, List<Integer>> getActionSeatMap() {
        return actionSeatMap;
    }

    public boolean isFirstCard() {
        return firstCard;
    }

    public void setFirstCard(boolean firstCard) {
        this.firstCard = firstCard;
        changeExtend();
    }

    /**
	 * ???????????????cardId-seat
	 */
    public KeyValuePair<Integer, Integer> getMoSeatPair() {
        return moSeatPair;
    }

    public PaohzCard getBeRemoveCard() {
        return beRemoveCard;
    }

    /**
	 * ?????????????????????
	 */
    public void setBeRemoveCard(PaohzCard beRemoveCard) {
        this.beRemoveCard = beRemoveCard;
        changeExtend();
    }

    /**
	 * ???????????????????????????
	 */
    public boolean isMoByPlayer(SyPaohuziPlayer player) {
        if (moSeatPair != null && moSeatPair.getValue() == player.getSeat()) {
            if (nowDisCardIds != null && !nowDisCardIds.isEmpty()) {
                if (nowDisCardIds.get(0).getId() == moSeatPair.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
        changeExtend();
    }

    public int getShuXingSeat() {
        return shuXingSeat;
    }

    public void setShuXingSeat(int shuXingSeat) {
        this.shuXingSeat = shuXingSeat;
    }

    public boolean isSiRenBoPi() {
        return isBoPi() && 4 == getMaxPlayerCount();
    }

    @Override
    public int getDissPlayerAgreeCount() {
        return getPlayerCount();
    }

    @Override
    public void createTable(Player player, int play, int bureauCount, List<Integer> params, List<String> strParams,
                            Object... objects) throws Exception {
		// // ??????????????????
//        if (PaohuziConstant.isPlayBopi(play)) {
//            if (params.size()>= 11) {
//                setCeiling(params.get(10));
//            }
////            setCeiling(150);
//        }
        createTable(player, play, bureauCount, params);
    }

    @Override
    public void createTable(Player player, int play, int bureauCount, Object... objects) throws Exception {
    }


	@Override
    public void calcDataStatistics2() {
		// ??????????????? ???????????????????????????????????????????????????????????????????????????????????????????????? ????????????
        if(isGroupRoom()){
            String groupId=loadGroupId();
            int maxPoint=0;
            int minPoint=0;
            Long dataDate=Long.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
            //Long dataDate, String dataCode, String userId, String gameType, String dataType, int dataValue

            calcDataStatistics3(groupId);

            for (SyPaohuziPlayer player:playerMap.values()){
				// ????????????
                DataStatistics dataStatistics1=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"xjsCount",playedBureau);
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics1,3);
                int finalPoint;
                if (isBoPi()) {
                    finalPoint = getBopiPoint(player);
                } else {
                    finalPoint = player.loadScore();
                }

				// ????????????
                DataStatistics dataStatistics5=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"djsCount",1);
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics5,3);
				// ?????????
                DataStatistics dataStatistics6=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"zjfCount",finalPoint);
                DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics6,3);

                if (finalPoint >0){
                    if (finalPoint >maxPoint){
                        maxPoint= finalPoint;
                    }
					// ??????????????????
                    DataStatistics dataStatistics2=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"winMaxScore", finalPoint);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics2,4);
                }else if (finalPoint <0){
                    if (finalPoint <minPoint){
                        minPoint= finalPoint;
                    }
					// ??????????????????
                    DataStatistics dataStatistics3=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"loseMaxScore", finalPoint);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics3,5);
                }
            }

            for (SyPaohuziPlayer player:playerMap.values()){
                int finalPoint;
                if (isBoPi()) {
                    finalPoint = getBopiPoint(player);
                } else {
                    finalPoint = player.loadScore();
                }
                if (maxPoint>0&&maxPoint== finalPoint){
					// ??????????????????
                    DataStatistics dataStatistics4=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"dyjCount",1);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics4,1);
                }else if (minPoint<0&&minPoint== finalPoint){
					// ??????????????????
                    DataStatistics dataStatistics5=new DataStatistics(dataDate,"group"+groupId,String.valueOf(player.getUserId()),String.valueOf(playType),"dfhCount",1);
                    DataStatisticsDao.getInstance().saveOrUpdateDataStatistics(dataStatistics5,2);
                }
            }
        }
    }

    public long saveUserGroupPlaylog() {
        if(!needSaveUserGroupPlayLog()){
            return 0;
        }
        UserGroupPlaylog userGroupLog = new UserGroupPlaylog();
        userGroupLog.setTableid(id);
        userGroupLog.setUserid(creatorId);
        userGroupLog.setCount(playBureau);
        String players = "";
        String score = "";
        String diFenScore = "";
        for (SyPaohuziPlayer player : seatMap.values()) {
            players += player.getUserId() + ",";
            if (isBoPi()) {
                score += player.getWinLossPoint() + ",";
                diFenScore += player.getWinLossPoint() + ",";
            } else {
                score += player.getTotalPoint() + ",";
                diFenScore += player.getTotalPoint() + ",";
            }
        }
        userGroupLog.setPlayers(players.length() > 0 ? players.substring(0, players.length() - 1) : "");
        userGroupLog.setScore(score.length() > 0 ? score.substring(0, score.length() - 1) : "");
        userGroupLog.setDiFenScore(diFenScore.length() > 0 ? diFenScore.substring(0, diFenScore.length() - 1) : "");
        userGroupLog.setDiFen("");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        userGroupLog.setCreattime(sdf.format(createTime));
        userGroupLog.setOvertime(sdf.format(new Date()));
        userGroupLog.setPlayercount(maxPlayerCount);
        userGroupLog.setGroupid(Long.parseLong(loadGroupId()));
        userGroupLog.setGamename(getGameName());
        userGroupLog.setTotalCount(totalBureau);
        return TableLogDao.getInstance().saveGroupPlayLog(userGroupLog);
    }

    @Override
    public String getGameName() {
        String res = "";
        if (playType == 32) {
			res = "????????????";
        } else if (playType == 33) {
			res = "????????????";
        }
        return res;
    }


    public int getAutoTimeOut() {
        return autoTimeOut;
    }

    public int getAutoTimeOut2() {
        return autoTimeOut2;
    }

    @Override
    public boolean isCreditTable(List<Integer> params){
        return params != null && params.size() > 15 && StringUtil.getIntValue(params, 15, 0) == 1;
    }

    public static final List<Integer> wanfaList = Arrays.asList(GameUtil.play_type_shaoyang,GameUtil.play_type_bopi);

    public static void loadWanfaTables(Class<? extends BaseTable> cls){
        for (Integer integer:wanfaList){
            TableManager.wanfaTableTypesPut(integer,cls);
        }
    }

    @Override
    public int getLogGroupTableBureau() {
        if (isBoPi()) {
            if (getTotalBureau() == 1) {
                return 1;
            }
            return 100;
        } else {
            return super.getLogGroupTableBureau();
        }
    }

    private Map<Integer, TempAction> loadTempActionMap(String json) {
        Map<Integer, TempAction> map = new ConcurrentHashMap<>();
        if (json == null || json.isEmpty())
            return map;
        JSONArray jsonArray = JSONArray.parseArray(json);
        for (Object val : jsonArray) {
            String str = val.toString();
            TempAction tempAction = new TempAction();
            tempAction.initData(str);
            map.put(tempAction.getSeat(), tempAction);
        }
        return map;
    }

    public void setDataForPlayLogTable(PlayLogTable logTable) {
        StringJoiner players = new StringJoiner(",");
        StringJoiner scores = new StringJoiner(",");
        for (int seat = 1, length = getSeatMap().size(); seat <= length; seat++) {
            SyPaohuziPlayer player = seatMap.get(seat);
            players.add(String.valueOf(player.getUserId()));
            if (isBoPi()) {
                scores.add(String.valueOf(player.getWinLossPoint()));
            } else {
                scores.add(String.valueOf(player.getTotalPoint()));
            }
        }
        logTable.setPlayers(players.toString());
        logTable.setScores(scores.toString());
    }

    public String getTableMsg() {
        Map<String, Object> json = new HashMap<>();
        if (isBoPi()) {
			json.put("wanFa", "????????????");
        } else {
			json.put("wanFa", "????????????");
        }
        if (isGroupRoom()) {
            json.put("roomName", getRoomName());
        }
        json.put("playerCount", getPlayerCount());
        if (isBoPi()) {
            json.put("count", 0);
        } else {
            json.put("count", getTotalBureau());
        }
        if (this.autoPlay) {
            json.put("autoTime", autoTimeOut / 1000);
            if (autoPlayGlob == 1) {
				json.put("autoName", "??????");
            } else {
				json.put("autoName", "??????");
            }
        }
        return JSON.toJSONString(json);
    }

    @Override
    public String getTableMsgForXianLiao() {
        StringBuilder sb = new StringBuilder();
		sb.append("???").append(getId()).append("???").append(finishBureau).append("/").append(totalBureau).append("???")
				.append("\n");
		sb.append("????????????????????????????????????????????????").append("\n");
		sb.append("???").append(getRoomName()).append("???").append("\n");
		sb.append("???").append(getGameName()).append("???").append("\n");
		sb.append("???").append(TimeUtil.formatTime(new Date())).append("???").append("\n");
        int maxPoint = -999999999;
        List<SyPaohuziPlayer> players = new ArrayList<>();
        for (SyPaohuziPlayer player : seatMap.values()) {
            int point = player.loadScore();
            if (isBoPi()) {
                point = getBopiPoint(player);
            }
            if (point > maxPoint) {
                maxPoint = point;
            }
            players.add(player);
        }
        Collections.sort(players, new Comparator<SyPaohuziPlayer>() {
            @Override
            public int compare(SyPaohuziPlayer o1, SyPaohuziPlayer o2) {
                if (isBoPi()) {
                    return getBopiPoint(o2) - getBopiPoint(o1);
                }else{
                    return o2.loadScore() - o1.loadScore();
                }
            }
        });
        for (SyPaohuziPlayer player : players) {
			sb.append("????????????????????????????????????????????????").append("\n");
            int point = player.loadScore();
            if (isBoPi()) {
                point = getBopiPoint(player);
            }
			sb.append(StringUtil.cutHanZi(player.getName(), 5)).append("???").append(player.getUserId()).append("???")
					.append(point == maxPoint ? "????????????" : "").append("\n");
            sb.append(point > 0 ? "+" : point == 0 ? "" : "-").append(Math.abs(point)).append("\n");
        }
        return sb.toString();
    }

    /**
     *
     * @param debugPaohzVal
     */
    public void debugTable(int debugPaohzVal ,SyPaohuziPlayer player) {
        if(!isGroupRoom()|| !player.groupTableDebugPermission(groupId,GameUtil.play_type_bopi)){
            return;
        }
        if(null!= this.getLeftCards() && this.getLeftCards().size()>0){
            List<PaohzCard> debugphz =   PaohuziTool.findPhzByVal(getLeftCards(), debugPaohzVal );
            if(null!=debugphz && !debugphz.isEmpty() ){
                if(gmDebugVal==null){
                    gmDebugVal= new ArrayList<>();
                }
                PaohzCard c=debugphz.get(0);
                gmDebugVal.add(c.getVal());
                this.gmDebugUserId = player.getUserId();
                return;
            }
        }
    }

    public synchronized void getLeftIds(SyPaohuziPlayer player) {
        if(null==this.getLeftCards()|| this.getLeftCards().isEmpty()){
            return;
        }
        if(!isGroupRoom()|| !player.groupTableDebugPermission(groupId,GameUtil.play_type_bopi)){
            return;
        }
        List<PaohzCard> phzs = new ArrayList<>(this.getLeftCards());
        HashMap<Integer, Integer> val_numMap = new HashMap<>();
        for (PaohzCard card : phzs) {
            if(val_numMap.containsKey(card.getVal())){
                int num = val_numMap.get(card.getVal());
                val_numMap.put(card.getVal(),++num);
            }else{
                val_numMap.put(card.getVal(),1);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int key:   val_numMap.keySet()  ) {
            sb.append(key).append(",").append(val_numMap.get(key)).append("|") ;
        }
        System.out.println(sb.toString());
        player.writeComMessage(WebSocketMsgType.req_code_leftIds,sb.toString());

    }
}
