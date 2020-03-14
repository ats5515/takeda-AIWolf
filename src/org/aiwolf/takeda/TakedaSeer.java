package org.aiwolf.takeda;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

/**
 * �?�?師役エージェントクラス
 */
public class TakedaSeer extends TakedaBasePlayer {
	int comingoutDay;
	boolean isCameout;

	Judge divination;
	
	boolean[] divined;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	boolean pos;
	boolean update_sh = true;
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if(f){
			params = new Parameters(numAgents);
			
			sh = new StateHolder(numAgents);
			
			f=false;
		}
		update_sh=true;
		pos = false;
		doCO = false;
		houkoku = true;
		
		divined = new boolean[numAgents];
		for(int i=0;i<numAgents;i++)divined[i] = false;
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint,numAgents,Util.SEER,params);
		
		before = -1;
		
	}
	
	public void dayStart() {
		super.dayStart();
		divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			divined[divination.getTarget().getAgentIdx() - 1] = true;
			houkoku = false;
			gamedata.add(new GameData(DataType.DIVINED, 
					day, meint, 
					divination.getTarget().getAgentIdx() - 1, 
					divination.getResult() == Species.HUMAN));
		}
		sh.process(params, gamedata);
	}


	protected void init() {

	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));
		
		sh.process(params, gamedata);
		
		int c = chooseMostLikelyWerewolf();
		
		return currentGameInfo.getAgentList().get(c);
	}
	protected String chooseTalk() {
		gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
		
		sh.process(params, gamedata);
		
		updateState(sh);
		
		if(update_sh){
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
		}
		
		double mn = -1;
		int c = 0;
		
		for(int i=0;i<numAgents;i++){
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		c = chooseMostLikelyWerewolf();
		if(getAliveAgentsCount() <= 3){
			if(!pos){
				pos = true;
				double all = 0;
				double alive = 0;
				for(int i=0;i<numAgents;i++){
					all+=sh.rp.getProb(i, Util.POSSESSED);
					if(sh.gamestate.agents[i].Alive){
						alive +=sh.rp.getProb(i, Util.POSSESSED);
					}
				}
				if(alive > 0.5 * all){
					doCO=true;
					houkoku = true;
					return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
				}
			}
		}
		
		if(!doCO){
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();

		}
		if(!houkoku){
			houkoku = true;
			
			if(numAgents == 5 && day == 1){
				return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c),
						Species.WEREWOLF ))).getText();
			}else{
				return (new Content(new DivinedResultContentBuilder(divination.getTarget(),
						divination.getResult()) )).getText();
			}
		}
		//if (before != c) {
		if(true){
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		before = c;
		return Talk.SKIP;
	}

	public Agent divine() {
		sh.process(params, gamedata);
		sh.update();
		double mn = -1;
		int c = -1;
	
		for(int i=0;i<numAgents;i++){
			if(i!=meint){
				if(sh.gamestate.agents[i].Alive){
					if(!divined[i]){
						double score = sh.rp.getProb(i, Util.WEREWOLF);
						if(mn < score){
							mn = score;
							c = i;
						}
					}
				}
			}
		}

		if (c == -1) return null;
		return currentGameInfo.getAgentList().get(c);
	}

}