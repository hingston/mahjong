package com.github.blovemaple.mj.cli;

import com.github.blovemaple.mj.action.Action;
import com.github.blovemaple.mj.action.ActionType;
import com.github.blovemaple.mj.action.PlayerAction;
import com.github.blovemaple.mj.action.PlayerActionType;
import com.github.blovemaple.mj.cli.CliView.CharHandler;
import com.github.blovemaple.mj.game.GameContextPlayerView;
import com.github.blovemaple.mj.object.Player;
import com.github.blovemaple.mj.object.Tile;
import com.github.blovemaple.mj.utils.LanguageManager.Message;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.github.blovemaple.mj.action.standard.AutoActionTypes.DEAL;
import static com.github.blovemaple.mj.action.standard.PlayerActionTypes.*;
import static com.github.blovemaple.mj.cli.CliGameView.TILE_COMPARATOR;
import static com.github.blovemaple.mj.cli.CliView.CharHandler.HandlingResult.*;
import static com.github.blovemaple.mj.utils.LanguageManager.ExtraMessage.*;
import static com.github.blovemaple.mj.utils.LanguageManager.message;

/**
 * 命令行界面的本地玩家。-> Local player on the command line interface.
 *
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class CliPlayer implements Player {
    private static final Logger logger = Logger.getLogger(CliPlayer.class.getSimpleName());
    private static final char PREVIOUS_CHOICE_KEY = ',';
    private static final char NEXT_CHOICE_KEY = '.';
    private static final char ACTION_KEY = ' ';
    private static final char ACTION_2_KEY = 'm';
    private static final char WIN_ACTION_KEY = 'h';
    private static final char PASS_KEY = '/';
    @SuppressWarnings("unused")
    private static final Message MOVE_CHOICE_KEY_MESSAGE = COMMA_AND_PERIOD_KEY;
    private static final Message ACTION_KEY_MESSAGE = SPACE_KEY;
    private static final Message ACTION_2_KEY_MESSAGE = M_KEY;
    private static final Message WIN_ACTION_KEY_MESSAGE = H_KEY;
    private static final Message PASS_KEY_MESSAGE = SLASH_KEY;
    private static final Comparator<Set<Tile>> TILES_CHOICE_COMPARATOR = (set1, set2) -> {
        List<Tile> list1 = set1.stream().sorted(TILE_COMPARATOR).collect(Collectors.toList());
        List<Tile> list2 = set2.stream().sorted(TILE_COMPARATOR).collect(Collectors.toList());
        int size = Math.min(list1.size(), list2.size());
        for (int i = 0; i < size; i++) {
            int c = TILE_COMPARATOR.compare(list1.get(i), list2.get(i));
            if (c != 0)
                return c;
        }
        return list1.size() < list2.size() ? -1 : list1.size() > list2.size() ? 1 : 0;
    };
    private final Map<Set<Tile>, List<ActionType>> actionChoices = new HashMap<>();
    private final ArrayList<Set<Tile>> actionTilesChoices = new ArrayList<>();
    private String name;
    private CliGameView view;
    private GameContextPlayerView contextView;
    private Set<Tile> focusedChoice;
    private boolean canChooseWin;
    private boolean canPass;
    // 选择了动作就放在这里 -> I chose to move here.
    private PlayerAction choseAction;
    private CharHandler choosingHandler = c -> {
        switch (c) {
            case PREVIOUS_CHOICE_KEY:
                moveFocusChoice(false);
                return ACCEPT;
            case NEXT_CHOICE_KEY:
                moveFocusChoice(true);
                return ACCEPT;
            case ACTION_KEY:
                if (focusedChoice != null) {
                    choseAction = new PlayerAction(contextView.getMyLocation(), actionChoices.get(focusedChoice).get(0), focusedChoice);
                    return QUIT;
                } else {
                    return IGNORE;
                }
            case ACTION_2_KEY:
                if (focusedChoice != null && actionChoices.get(focusedChoice).size() > 1) {
                    choseAction = new PlayerAction(contextView.getMyLocation(), actionChoices.get(focusedChoice).get(1), focusedChoice);
                    return QUIT;
                } else {
                    return IGNORE;
                }
            case WIN_ACTION_KEY:
                if (canChooseWin) {
                    choseAction = new PlayerAction(contextView.getMyLocation(), WIN);
                    return QUIT;
                } else {
                    return IGNORE;
                }
            case PASS_KEY:
                if (canPass) {
                    choseAction = null;
                    return QUIT;
                } else {
                    return IGNORE;
                }
            default:
                return IGNORE;
        }

    };

    public CliPlayer(String name, CliView cliView) {
        this.name = name;
        this.view = new CliGameView(cliView);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized PlayerAction chooseAction(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes, PlayerAction illegalAction) throws InterruptedException {
        // 如果可以补花，则自动补花 -> If you can add flowers, then automatically add flowers
        Collection<Set<Tile>> buhuas = BUHUA.getLegalActionTiles(contextView);
        if (!buhuas.isEmpty()) {
            return new PlayerAction(contextView.getMyLocation(), BUHUA, buhuas.iterator().next());
        }

        // 如果可以吃/碰/杠/和，则提供选择 -> Provide options if you can eat / touch / bar / and
        boolean canWin = actionTypes.stream().anyMatch(WIN::matchBy);
        PlayerAction action = chooseAction(contextView, actionTypes, canWin, true,
                CHI, PENG, ZHIGANG, BUGANG, ANGANG);
        if (action != null)
            return action;

        // 如果可以打牌，则提供选择 -> Provide options if you can play cards
        action = chooseAction(contextView, actionTypes, false, false, DISCARD, DISCARD_WITH_TING);
        if (action != null)
            return action;

        // 如果可以摸牌，则自动摸牌 -> If you can touch the card, you will automatically draw the card.
        for (ActionType drawType : Arrays.asList(DRAW, DRAW_BOTTOM))
            if (actionTypes.contains(drawType))
                return new PlayerAction(contextView.getMyLocation(), drawType);

        // 啥都没做，放弃了 -> I didn't do it, I gave up.
        return null;
    }

    private PlayerAction chooseAction(GameContextPlayerView contextView, Set<PlayerActionType> legalActionTypes, boolean canChooseWin, boolean canPass, PlayerActionType... chooseActionTypes) throws InterruptedException {
        this.contextView = contextView;

        Arrays.asList(chooseActionTypes).stream().filter(legalActionTypes::contains).forEach(actionType -> {
            actionType.getLegalActionTiles(contextView).forEach(tiles -> {
                List<ActionType> types = actionChoices.get(tiles);
                if (types == null) {
                    types = new ArrayList<>();
                    actionChoices.put(tiles, types);
                    actionTilesChoices.add(tiles);
                }
                types.add(actionType);
            });

        });

        if (actionChoices.isEmpty() && !canChooseWin)
            return null;

        if (!actionChoices.isEmpty()) {
            // 先按照正常顺序排一下 -> First arrange in the normal order
            actionTilesChoices.sort(TILES_CHOICE_COMPARATOR);

            // 决定一开始focus的牌： -> Decide on the card that starts to focus:
            // 如果刚摸牌，则focus刚摸的牌，否则，如果要打牌则focus最后一张，否则focus第一张 -> If you just touch the card,
            // the focus just touches the card. Otherwise, if you want to play the card, the focus is the last one,
            // otherwise the focus is the first one.
            Tile justDrawed = contextView.getJustDrawedTile();
            if (justDrawed != null) {
                Set<Tile> justDrawedSet = Collections.singleton(justDrawed);
                if (actionChoices.containsKey(justDrawedSet)) {
                    focusedChoice = justDrawedSet;
                    // 把刚摸的牌排到最后去 -> Put the cards you just touched to the end
                    actionTilesChoices.remove(justDrawedSet);
                    actionTilesChoices.add(justDrawedSet);
                }
            }
            if (focusedChoice == null) {
                Set<Tile> lastChoice = actionTilesChoices.get(actionTilesChoices.size() - 1);
                if (actionChoices.get(lastChoice).contains(DISCARD))
                    focusedChoice = lastChoice;
                else
                    focusedChoice = actionTilesChoices.get(0);
            }

        }

        this.canChooseWin = canChooseWin;
        this.canPass = canPass;
        viewFocusAndOptions();

        choseAction = null;
        try {
            view.getCliView().addCharHandler(choosingHandler, true);
        } finally {
            actionChoices.clear();
            actionTilesChoices.clear();
            focusedChoice = null;
            this.canChooseWin = false;
            this.canPass = false;
            viewFocusAndOptions();
        }

        return choseAction;
    }

    private void moveFocusChoice(boolean forward) {
        if (actionTilesChoices.isEmpty())
            return;
        try {
            focusedChoice = actionTilesChoices.get(actionTilesChoices.indexOf(focusedChoice) + (forward ? 1 : -1));
        } catch (IndexOutOfBoundsException e) {
            focusedChoice = actionTilesChoices.get(forward ? 0 : actionTilesChoices.size() - 1);
        }
        viewFocusAndOptions();
    }

    private void viewFocusAndOptions() {
        view.setFocusedAliveTiles(focusedChoice);

        List<ActionType> actionTypeList = focusedChoice == null ? Collections.emptyList() : actionChoices.get(focusedChoice);
        if (actionTypeList.size() > 2)
            throw new RuntimeException("Action types > 2 : " + actionTypeList);

        // 手机所有要显示的options，要按顺序，所以用Linked -> All the options to be displayed on the phone, in order, so use Linked
        Map<Message, Message> options = new LinkedHashMap<>();
        // 不显示移动的按键提示了，因为会太宽 -> Do not display the moving button prompts because it will be too wide
        // if (actionChoices.size() > 1)
        // options.put(MOVE_CHOICE_KEY_MESSAGE, MOVE_CHOICE);
        if (!actionTypeList.isEmpty())
            options.put(ACTION_KEY_MESSAGE, message(actionTypeList.get(0)));
        if (actionTypeList.size() > 1)
            options.put(ACTION_2_KEY_MESSAGE, message(actionTypeList.get(1)));
        if (canChooseWin)
            options.put(WIN_ACTION_KEY_MESSAGE, message(WIN));
        if (canPass)
            options.put(PASS_KEY_MESSAGE, PASS);
        view.setOptions(options);
    }

    @Override
    public void timeLimit(GameContextPlayerView contextView, Integer secondsToGo) {
        view.setTimeLimit(secondsToGo);
    }

    @Override
    public void actionDone(GameContextPlayerView contextView, Action action) {
        try {
            if (DEAL.matchBy(action.getType()))
                view.setContext(contextView);
            view.showAction(action);
        } catch (IOException e) {
            try {
                logger.log(Level.SEVERE, e.toString(), e);
                view.getCliView().printMessage("[ERROR] " + e.toString());
            } catch (IOException e1) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

}
