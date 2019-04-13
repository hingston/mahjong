package com.github.blovemaple.mj.action.standard;

import com.github.blovemaple.mj.action.AbstractPlayerActionType;
import com.github.blovemaple.mj.game.GameContext;
import com.github.blovemaple.mj.game.GameContextPlayerView;
import com.github.blovemaple.mj.object.PlayerInfo;
import com.github.blovemaple.mj.object.PlayerLocation;
import com.github.blovemaple.mj.object.Tile;
import com.github.blovemaple.mj.object.TileGroup;

import java.util.Set;
import java.util.function.Predicate;

import static com.github.blovemaple.mj.object.TileGroupType.ANGANG_GROUP;

/**
 * 动作类型“暗杠”。
 *
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class AngangActionType extends AbstractPlayerActionType {

    protected AngangActionType() {
    }

    @Override
    public boolean canPass(GameContext context, PlayerLocation location) {
        return true;
    }

    @Override
    protected Predicate<Integer> getAliveTileSizePrecondition() {
        return size -> size % 3 == 2;
    }

    @Override
    protected int getActionTilesSize() {
        return ANGANG_GROUP.size();
    }

    @Override
    protected boolean isLegalActionWithPreconition(GameContextPlayerView context,
                                                   Set<Tile> tiles) {
        return ANGANG_GROUP.isLegalTiles(tiles);
    }

    @Override
    protected void doLegalAction(GameContext context, PlayerLocation location,
                                 Set<Tile> tiles) {
        PlayerInfo playerInfo = context.getPlayerInfoByLocation(location);
        playerInfo.getAliveTiles().removeAll(tiles);
        playerInfo.getTileGroups().add(new TileGroup(ANGANG_GROUP, tiles));
    }

}
