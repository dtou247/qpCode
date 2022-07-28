package com.sy599.game.gcommand.com;

import com.sy599.game.character.Player;
import com.sy599.game.common.constant.LangMsg;
import com.sy599.game.db.bean.group.GroupUser;
import com.sy599.game.db.bean.redBagRain.GroupRedBagRainConfig;
import com.sy599.game.db.bean.redBagRain.GroupRedBagResult;
import com.sy599.game.db.dao.GroupRedBagRainDao;
import com.sy599.game.db.dao.group.GroupDao;
import com.sy599.game.gcommand.BaseCommand;
import com.sy599.game.msg.serverPacket.ComMsg.ComReq;
import com.sy599.game.util.GroupRedBagRainUtil;
import com.sy599.game.util.LangHelp;
import com.sy599.game.util.LogUtil;
import com.sy599.game.util.ResourcesConfigsUtil;
import com.sy599.game.websocket.constant.WebSocketMsgType;
import com.sy599.game.websocket.netty.coder.MessageUnit;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GroupRedBagRainCommand extends BaseCommand {
    private static final int getOneRedBag=1;


    @Override
    public void execute(Player player, MessageUnit message) throws Exception {
        ComReq req = (ComReq) this.recognize(ComReq.class, message);
        List<Integer> lists = req.getParamsList();
        if (lists == null || lists.size() == 0) {
            player.writeErrMsg(LangHelp.getMsg(LangMsg.code_3));
            return;
        }
        // 获得传递过来的操作指令
        int command = req.getParams(0);
        switch (command){
            case getOneRedBag:
                int redbagconfigId = req.getParams(1);
                int isHuiZhang =req.getParams(2);//1=会长
                //System.err.println("======"+player.getName()+" redbagconfigId= "+redbagconfigId);
                  getOneRedBag(player,redbagconfigId,isHuiZhang);
                break;

        }
    }

    private synchronized void getOneRedBag(Player player, int redbagconfigId,int isHuiZhang) {

//        int control =   ResourcesConfigsUtil.loadIntegerValue("RedBagConfig","isOpen");
//        //System.err.println("control="+control);
//        if(control!=1){
//            //全部关闭
//            player.writeErrMsg("活动已关闭");
//            return;
//        }
        HashMap<String ,Object> map = new HashMap<>();
        map.put("keyId",redbagconfigId);
        map.put("userId",player.getUserId());
        map.put("userName",player.getName());
        GroupRedBagRainConfig config =GroupRedBagRainUtil.needPushRedMapRainConfigMap.get(""+redbagconfigId);
        //System.err.println("======"+player.getName()+" config= "+config.toString());
        if(config==null || config.getState()!=1){
            //'0未开始1发放中2已结束3已结算',
            player.writeErrMsg("红包没了~~");
            return;
        }
        try {
            GroupRedBagResult result = GroupRedBagRainDao.getInstance().getRedBagNum(map);
            if(result==null){
                int isGet = GroupRedBagRainDao.getInstance().getOneRedBag(map);
                //System.err.println("======"+player.getName()+" isGet= "+isGet);
                if(isGet == 1){
                    //抢到了
                    result = GroupRedBagRainDao.getInstance().getRedBagNum(map);
                    player.writeComMessage(WebSocketMsgType.resp_code_GroupRedBagRain_take,result.getRedbagnum()+"");
                    //
                    //System.err.println("======"+player.getName()+" isGet= "+result.getRedbagnum());
                    if(isHuiZhang==1){
                        //会长拿红包 直接入账
                        //GroupUser u2=GroupDao.getInstance().loadGroupUser(player.getUserId(),result.getGroupid());

                        GroupRedBagRainDao.getInstance().updateHuiZhangRecordResultState(map);

                        GroupRedBagRainUtil.addCreditLog(config.getGroupid(),player.getUserId(),result.getRedbagnum());

                        GroupDao.getInstance().updateGroupCredit(String.valueOf(result.getGroupid()),player.getUserId(), result.getRedbagnum());

                       // GroupUser u1=GroupDao.getInstance().loadGroupUser(player.getUserId(),result.getGroupid());
                        //System.err.println("前.get:"+u2.getCredit());
                        //System.err.println("后.get:"+u1.getCredit());
                        //System.err.println("======群主："+player.getName()+" isGet= "+result.getRedbagnum());
                    }
                }else{
                    player.writeComMessage(WebSocketMsgType.resp_code_GroupRedBagRain_take,"0");
                }
            }else{
                player.writeErrMsg("只能拿1次哦~~");
                return;
            }
        }catch (Exception e){
            player.writeErrMsg("系统异常！");
            LogUtil.errorLog.info("GroupRedBagRainCommand|e:",e);
        }
    }
    @Override
    public void setMsgTypeMap() {

    }

}
