import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;

public class ChessDisplay extends Application {
    private HashMap<String, Image> pieceSprites;
    private ChessParent[] grid;
    private ChessGame chess;
    private String currentSelection = "";
    private ImageView iv;
    private ArrayList<Long> timeTally;
    // private boolean blackInverted;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        iv = new ImageView();
        iv.setImage(new Image("sprites/board_grey.png", 600, 600, true, true));
        pieceSprites = new HashMap<>();
        timeTally = new ArrayList<>();
        String[] array = "KQBNRPACSD".split("");
        for (String character : array) {
            String substitute = "SCD".contains(character) ? "K" : "A".equals(character) ? "P" : character;
            pieceSprites.put("w" + character, new Image("/sprites/w" + substitute + ".png"));
            pieceSprites.put("b" + character, new Image("/sprites/b" + substitute + ".png"));
        }

        grid = new ChessParent[64];
        for (int i = 0; i < 64; i++) {
            grid[i] = new ChessParent(i / 8, 7 - (i % 8));
            //noinspection IntegerDivisionInFloatingPointContext
            grid[i].relocate(75 * (i / 8), 75 * (i % 8));
            String squareName = grid[i].name;
            grid[i].setOnMouseClicked(e -> {
                String validMoves = chess.getValidMoves(squareName);
                if ((chess.squareIsEmpty(squareName) && currentSelection.equals("")) || squareName.equals(currentSelection)) {
                    currentSelection = "";
                    updateScreen("");
                } else if (!chess.squareIsEmpty(squareName) && currentSelection.equals("")) {
                    currentSelection = squareName;
                    if (validMoves.isEmpty()) {
                        currentSelection = "";
                    }
                    updateScreen(validMoves + currentSelection);
                } else if (!currentSelection.isEmpty()) {
                    if (!validMoves.isEmpty()) {
                        currentSelection = squareName;
                        updateScreen(validMoves + squareName);
                    } else {
                        long time = System.currentTimeMillis();
                        chess.move(currentSelection + squareName);
                        timeTally.add(System.currentTimeMillis() - time);
                        currentSelection = "";
                        updateScreen("");
                    }
                }
            });
        }

        MenuBar menuBar = new MenuBar();
        Menu optionsMenu = new Menu("Options");
        MenuItem takeBackItem = new MenuItem("Take back");
        takeBackItem.setOnAction(e -> {
            chess.takeBack();
            updateScreen("");
        });
        MenuItem resetBoardItem = new MenuItem("Reset");
        Menu coloursMenu = new Menu("Board color");
        MenuItem[] colours = new MenuItem[6];

        colours[0] = new MenuItem("Dark blue");
        colours[0].setOnAction(e -> changeBoardColour("main"));
        colours[1] = new MenuItem("Green");
        colours[1].setOnAction(e -> changeBoardColour("green"));
        colours[2] = new MenuItem("Red");
        colours[2].setOnAction(e -> changeBoardColour("red"));
        colours[3] = new MenuItem("Brown");
        colours[3].setOnAction(e -> changeBoardColour("brown"));
        colours[4] = new MenuItem("Grey");
        colours[4].setOnAction(e -> changeBoardColour("grey"));
        colours[5] = new MenuItem("Blue");
        colours[5].setOnAction(e -> changeBoardColour("blue"));

        coloursMenu.getItems().addAll(colours);

        resetBoardItem.setOnAction(e -> resetBoard());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        optionsMenu.getItems().addAll(takeBackItem, resetBoardItem, coloursMenu, new SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(optionsMenu);
        menuBar.setPrefWidth(600);

        chess = new ChessGame();
        Group chess_root = new Group(iv);
        chess_root.getChildren().addAll(grid);
        chess_root.relocate(0, 25);

        Group root = new Group();
        root.getChildren().addAll(chess_root, menuBar);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        updateScreen("");
        chess_root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                resetBoard();
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                takeBackItem.fire();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                System.exit(0);
            }
        });
        chess_root.requestFocus();
    }

    public void changeBoardColour(String newColour) {
        iv.setImage(new Image("sprites/board_" + newColour + ".png", 600, 600, true, true));
    }

    private void resetBoard() {
        chess = new ChessGame();
        for (ChessParent parent : grid) {
            parent.toggleValidDestination(false);
        }
        updateScreen("");
    }

    private void updateScreen(String validSquares) {
        String[] gameState = chess.getPresentableArray();
        for (int i = 0; i < 64; i++) {
            grid[i].toggleCheck(false);
            grid[i].toggleValidDestination(false);
            if (gameState[i].contains("A")) {
                chess.replacePromotablePiece("Q");
                gameState = chess.getPresentableArray();
            } else if (gameState[i].contains("S")) {
                System.out.println("Stalemate");
                printAverageTime();
            } else if (gameState[i].contains("D")) {
                System.out.println("Checkmate");
                printAverageTime();
                grid[i].toggleCheck(true);
            } else if (gameState[i].contains("C")) {
                grid[i].toggleCheck(true);
            }

            if (validSquares.contains(grid[i].name)) {
                grid[i].toggleValidDestination(true);
            }
            grid[i].setImage(pieceSprites.get(gameState[i]));
        }
    }

    public void printAverageTime() {
        long average = timeTally.stream().mapToLong(e -> e).sum() / timeTally.size();
        System.out.println("Average time taken per move is " + average + " ms");
    }

    private static class ChessParent extends Parent {
        public final String name;
        public final ImageView iv;
        private Rectangle rect;

        public ChessParent(int file, int rank) {
            name = "" + "abcdefgh".charAt(file) + (rank + 1);
            iv = new ImageView();
            rect = new Rectangle(75, 75, new Color(0, 0, 0, 0));
            getChildren().addAll(rect, iv);
        }

        public void setImage(Image image) {
            iv.setImage(image);
        }

        public void toggleValidDestination(boolean valid) {
            if (valid) {
                rect.setFill(new Color(0, 1, 0, 0.5));
            } else {
                rect.setFill(new Color(0, 0, 0, 0));
            }
        }

        public void toggleCheck(boolean isChecked) {
            if (isChecked) {
                rect.setFill(new Color(0.7, 0, 0, 1));
            } else {
                rect.setFill(new Color(0, 0, 0, 0));
            }
        }
    }
}