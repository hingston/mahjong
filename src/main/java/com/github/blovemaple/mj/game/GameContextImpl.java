package com.github.blovemaple.mj.game;

import com.github.blovemaple.mj.action.Action;
import com.github.blovemaple.mj.action.PlayerAction;
import com.github.blovemaple.mj.object.MahjongTable;
import com.github.blovemaple.mj.object.PlayerInfo;
import com.github.blovemaple.mj.object.PlayerLocation;
import com.github.blovemaple.mj.rule.GameStage;
import com.github.blovemaple.mj.rule.GameStrategy;
import com.github.blovemaple.mj.rule.TimeLimitStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * {@link GameContext}实现。
 *
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class GameContextImpl implements GameContext {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger
            .getLogger(GameContextImpl.class.getSimpleName());
    private final Map<PlayerLocation, GameContextPlayerView> playerViews = new HashMap<>();
    private MahjongTable table;
    private GameStrategy gameStrategy;
    private TimeLimitStrategy timeLimitStrategy;
    private PlayerLocation zhuangLocation;
    private GameStage stage;
    private List<Action> doneActions = new ArrayList<>();
    private GameResult gameResult;

    public GameContextImpl(MahjongTable table, GameStrategy gameStrategy, TimeLimitStrategy timeLimitStrategy) {
        this.table = table;
        this.gameStrategy = gameStrategy;
        this.timeLimitStrategy = timeLimitStrategy;
    }

    @Override
    public MahjongTable getTable() {
        return table;
    }

    @Override
    public GameStrategy getGameStrategy() {
        return gameStrategy;
    }

    @Override
    public TimeLimitStrategy getTimeLimitStrategy() {
        return timeLimitStrategy;
    }

    @Override
    public PlayerInfo getPlayerInfoByLocation(PlayerLocation location) {
        return table.getPlayerInfos().get(location);
    }

    @Override
    public PlayerLocation getZhuangLocation() {
        return zhuangLocation;
    }

    @Override
    public void setZhuangLocation(PlayerLocation zhuangLocation) {
        this.zhuangLocation = zhuangLocation;
    }

    @Override
    public GameStage getStage() {
        return stage;
    }

    @Override
    public void setStage(GameStage stage) {
        this.stage = stage;
    }

    @Override
    public void actionDone(Action action) {
        doneActions.add(action);
    }

    @Override
    public Action getLastAction() {
        return doneActions.isEmpty() ? null
                : doneActions.get(doneActions.size() - 1);
    }

    @Override
    public PlayerLocation getLastActionLocation() {
        Action lastAction = getLastAction();
        if (lastAction == null || !(lastAction instanceof PlayerAction))
            return null;
        return ((PlayerAction) lastAction).getLocation();
    }

    @Override
    public List<Action> getDoneActions() {
        return doneActions;
    }

    protected void setDoneActions(List<Action> doneActions) {
        this.doneActions = doneActions;
    }

    @Override
    public GameResult getGameResult() {
        return gameResult;
    }

    @Override
    public void setGameResult(GameResult gameResult) {
        this.gameResult = gameResult;
    }

    @Override
    public GameContextPlayerView getPlayerView(PlayerLocation location) {
        GameContextPlayerView view = playerViews.get(location);
        if (view == null) { // 不需要加锁，因为多创建了也没事
            view = newPlayerView(location);
            playerViews.put(location, view);
        }
        return view;
    }

    protected GameContextPlayerView newPlayerView(PlayerLocation location) {
        return new GameContextPlayerViewImpl(this, location);
    }

}
