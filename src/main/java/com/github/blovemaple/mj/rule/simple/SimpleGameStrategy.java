package com.github.blovemaple.mj.rule.simple;

import com.github.blovemaple.mj.game.GameContext;
import com.github.blovemaple.mj.object.PlayerLocation;
import com.github.blovemaple.mj.rule.AbstractGameStrategy;
import com.github.blovemaple.mj.rule.GameStage;
import com.github.blovemaple.mj.rule.win.FanType;
import com.github.blovemaple.mj.rule.win.WinType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 简单游戏规则。固定坐庄、没有和牌限制、和牌固定为1番。
 *
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class SimpleGameStrategy extends AbstractGameStrategy {

    private static final List<WinType> WIN_TYPES = Collections.singletonList(NormalWinType.get());
    private static final List<FanType> FAN_TYPES = Collections.singletonList(new SimpleFanType());
    private List<GameStage> stages = List.of(new DealingStage(), new BeforePlayingStage(), new PlayingStage(),
            new FinishedStage());

    @Override
    protected PlayerLocation nextZhuangLocation(GameContext context) {
        return PlayerLocation.EAST;
    }

    @Override
    public List<WinType> getAllWinTypes() {
        return WIN_TYPES;
    }

    @Override
    public List<FanType> getAllFanTypes() {
        return FAN_TYPES;
    }

    @Override
    protected Collection<GameStage> getAllStages() {
        return stages;
    }

    @Override
    public GameStage getFirstStage() {
        return stages.get(0);
    }

}
