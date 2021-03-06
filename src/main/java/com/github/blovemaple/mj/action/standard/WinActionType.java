package com.github.blovemaple.mj.action.standard;

import com.github.blovemaple.mj.action.AbstractPlayerActionType;
import com.github.blovemaple.mj.action.Action;
import com.github.blovemaple.mj.action.IllegalActionException;
import com.github.blovemaple.mj.action.PlayerAction;
import com.github.blovemaple.mj.game.GameContext;
import com.github.blovemaple.mj.game.GameContextPlayerView;
import com.github.blovemaple.mj.game.GameResult;
import com.github.blovemaple.mj.object.PlayerLocation;
import com.github.blovemaple.mj.object.Tile;
import com.github.blovemaple.mj.rule.win.FanType;
import com.github.blovemaple.mj.rule.win.WinInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.github.blovemaple.mj.action.standard.AutoActionTypes.DEAL;
import static com.github.blovemaple.mj.action.standard.PlayerActionTypes.DISCARD;
import static com.github.blovemaple.mj.action.standard.PlayerActionTypes.DRAW;
import static com.github.blovemaple.mj.utils.MyUtils.mergedSet;

/**
 * 动作类型“和牌”。
 *
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class WinActionType extends AbstractPlayerActionType {

    protected WinActionType() {
    }

    @Override
    public boolean canPass(GameContext context, PlayerLocation location) {
        return true;
    }

    @Override
    protected BiPredicate<Action, PlayerLocation> getLastActionPrecondition() {
        // 必须是发牌、自己摸牌，或别人打牌后
        return (a, location) -> DEAL.matchBy(a.getType()) || //
                (a instanceof PlayerAction && //
                        (((PlayerAction) a).getLocation() == location ? //
                                DRAW.matchBy(a.getType()) : DISCARD.matchBy(a.getType())));
    }

    @Override
    protected int getActionTilesSize() {
        return 0;
    }

    @Override
    public boolean isLegalActionWithPreconition(GameContextPlayerView context, Set<Tile> tiles) {
        Action lastAction = context.getLastAction();
        Tile winTile = lastAction instanceof PlayerAction ? ((PlayerAction) lastAction).getTile() : null;
        boolean ziMo = !DISCARD.matchBy(context.getLastAction().getType());
        WinInfo winInfo = WinInfo.fromPlayerTiles(context.getMyInfo(), winTile, ziMo);
        winInfo.setContextView(context);
        if (!ziMo)
            winInfo.setAliveTiles(mergedSet(context.getMyInfo().getAliveTiles(), winTile));
        return context.getGameStrategy().canWin(winInfo);
    }

    @Override
    // XXX - 为了避免验证legal和算番时重复判断和牌，doAction时不进行legal验证，需要此方法的调用方保证legal（目前已保证）。
    public void doAction(GameContext context, Action action) throws IllegalActionException {
        Tile winTile = ((PlayerAction) context.getLastAction()).getTile();
        boolean ziMo = !DISCARD.matchBy(context.getLastAction().getType());
        PlayerLocation location = ((PlayerAction) action).getLocation();

        GameResult result = new GameResult(context.getTable().getPlayerInfos(), context.getZhuangLocation());
        result.setWinnerLocation(location);
        if (ziMo) {
            result.setWinTile(context.getPlayerView(location).getJustDrawedTile());
        } else {
            result.setWinTile(winTile);
            result.setPaoerLocation(context.getLastActionLocation());
        }

        // 和牌parse units、算番
        WinInfo winInfo = WinInfo.fromPlayerTiles(context.getPlayerInfoByLocation(location), winTile, ziMo);
        winInfo.setContextView(context.getPlayerView(location));
        if (!ziMo)
            winInfo.setAliveTiles(mergedSet(context.getPlayerInfoByLocation(location).getAliveTiles(), winTile));
        Map<FanType, Integer> fans = context.getGameStrategy().getFans(winInfo);
        if (fans.isEmpty() && (winInfo.getUnits() == null || winInfo.getUnits().isEmpty()))
            throw new IllegalActionException(context, action);
        result.setFans(fans);

        context.setGameResult(result);
    }

    @Override
    protected void doLegalAction(GameContext context, PlayerLocation location, Set<Tile> tiles) {
        throw new UnsupportedOperationException();
    }

}
