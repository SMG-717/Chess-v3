import java.util.Stack;

/**
 * This is the core part of the chess game. It accepts String inputs in the form f1r1-f2r2
 * where f1, r1, f2, r2 are starting file, starting row, ending file and ending row respectively.
 * It has two forms of output, one for general display and one for game state and statistics.
 * Unlike previous versions, the core game is now contained in one class with a few extra inner classes.
 * The Move class simplifies carrying information about moves around. It also has a lost of accessor
 * methods for convenience. The Piece serves to carry information about the particular piece, namely
 * its name, colour and whether or not it has moved. Pieces do not have references to the board,
 * each other or their location; they simply hold a few fields. The square class holds pieces, information
 * about it's location on the board and a couple of accessor methods. It does not have references to
 * other squares or their contents. The main class also has a stack that stores previous game states
 * to allow for take-backs.
 *
 * DISCLAIMER:
 * For some reason the isLegalMove method has a bug that is resolved by calling the method twice.
 * I have made sure that the conditions are exactly the same every time it is called, yet the bug still persists.
 * Note that the bug happens only when called from the getLegalMoves and switchKingWithMate methods and so I've
 * temporarily fixed them by calling it again if it returns false.
 *
 * @author Saifeldin Mohamed
 * @version 3.1
 * @since 04/02/2020
 */

public class ChessGame {

    // The sets of vectors used when checking if a move is valid on an elementary basis
    private static final int[][]
            white_pawn_move = {{0, 1}, {0, 2}, {1, 1}, {-1, 1}},
            black_pawn_move = {{0, -1}, {0, -2}, {-1, -1}, {1, -1}},
            knight_move = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}},
            king_move = {{-1, 1}, {0, 1}, {1, 1}, {-1, 0}, {1, 0}, {-1, -1}, {0, -1}, {1, -1}},
            rook_move = {{1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0},
                    {-1, 0}, {-2, 0}, {-3, 0}, {-4, 0}, {-5, 0}, {-6, 0}, {-7, 0},
                    {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7},
                    {0, -1}, {0, -2}, {0, -3}, {0, -4}, {0, -5}, {0, -6}, {0, -7}},
            bishop_move = {{1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}, {6, 6}, {7, 7},
                    {-1, -1}, {-2, -2}, {-3, -3}, {-4, -4}, {-5, -5}, {-6, -6}, {-7, -7},
                    {-1, 1}, {-2, 2}, {-3, 3}, {-4, 4}, {-5, 5}, {-6, 6}, {-7, 7},
                    {1, -1}, {2, -2}, {3, -3}, {4, -4}, {5, -5}, {6, -6}, {7, -7}},
            queen_move = {{1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0},
                    {-1, 0}, {-2, 0}, {-3, 0}, {-4, 0}, {-5, 0}, {-6, 0}, {-7, 0},
                    {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7},
                    {0, -1}, {0, -2}, {0, -3}, {0, -4}, {0, -5}, {0, -6}, {0, -7},
                    {1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}, {6, 6}, {7, 7},
                    {-1, -1}, {-2, -2}, {-3, -3}, {-4, -4}, {-5, -5}, {-6, -6}, {-7, -7},
                    {-1, 1}, {-2, 2}, {-3, 3}, {-4, 4}, {-5, 5}, {-6, 6}, {-7, 7},
                    {1, -1}, {2, -2}, {3, -3}, {4, -4}, {5, -5}, {6, -6}, {7, -7}};

    // The board used to contain the pieces and a clone of it used when checking for threats
    private Square[][] board, nextBoard;

    // Contains information about the current game states
    private GameState gameState;

    // A Stack used to store previous game states for take-back purposes
    private Stack<GameState> gameStateStack;

    /**
     * The constructor used when initialising the game to a specific state encoded using fen
     *
     * @param state an fen chess format
     */
    public ChessGame(String state) {
        board = new Square[8][8];
        for (int i = 0; i < 64; i++) {
            board[i / 8][i % 8] = new Square(i / 8, i % 8);
        }
        setGameState(state);
        gameStateStack = new Stack<>();
        nextBoard = cloneBoard();
    }

    /**
     * This constructor is used when initialising the game to the default state
     */
    public ChessGame() {

        this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR_w_KQkq_-_0_1");
    }

    /**
     * Temporary method that prints board contents in ascii form for debugging
     */
    public void printOnScreen() {
        for (Square[] row : board) {
            for (Square square : row) {
                if (square.isEmpty()) {
                    System.out.print(" .");
                } else if (square.isWhite()) {
                    System.out.print(" " + square.getName().toUpperCase());
                } else {
                    System.out.print(" " + square.getName().toLowerCase());
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * The move method is the main method of communication with the game.
     * It accepts moves in the format fr-fr, eg, "d2d4".
     *
     * @param text moves in positional format
     */
    public void move(String text) {
        char[] a = text.toCharArray();
        Move move = new Move(a[1] - '1', a[0] - 'a', a[3] - '1', a[2] - 'a');
        if (isLegalMove(move)) {
            forcedPieceMove(move.makeReal());
            gameStateStack.push(gameState.toggleTurn());
            switchKingWithMate();
            // System.out.println(text);
        }
    }

    /**
     * Sets the game state when given a string in the fen format
     *
     * @param state state in fen format
     */
    private void setGameState(String state) {
        for (int i = 0; i < 64; i++) {
            board[i / 8][i % 8].setOccupant(null);
        }
        String[] fields = state.split("_");
        gameState = new GameState(fields[0], fields[2], getSquare(fields[3]), fields[1].equals("w"),
                Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
        setBoardFromGameState();
    }

    /**
     * Gets the piece arrangement from the game state in fen format and sets the board up
     */
    private void setBoardFromGameState() {
        String[] boardArrangement = gameState.arrangement.split("/");
        for (int i = 0; i < 64; i++) {
            board[i / 8][i % 8].removeOccupant();
        }
        for (int i = 7; i >= 0; i--) {
            int column = 0;
            char[] arguments = boardArrangement[7 - i].toCharArray();
            for (char character : arguments) {
                if (character - '0' <= 8) {
                    column += character - '0';
                } else if (character < 'Z' && column < 8) {
                    board[i][column].setOccupant(new Piece("w" + character));
                    column++;
                } else if (column < 8) {
                    board[i][column].setOccupant(new Piece("b" + character));
                    column++;
                }
            }
        }
    }

    /**
     * Goes back one step to the previous game state
     */
    public void takeBack() {
        if (!gameStateStack.isEmpty()) {
            gameState = gameStateStack.pop();
            setBoardFromGameState();
        }
    }

    /**
     * Checks whether the move provided is a legal move or not in a complete sense.
     *
     * @param move move to be queried
     * @return legal or not
     */
    private boolean isLegalMove(Move move) {
        nextBoard = cloneBoard();
        Piece startPiece = move.startPiece();
        Piece endPiece = move.endPiece();
        if (gameState.isWhite != move.isWhite || startPiece == null
                || (endPiece != null && startPiece.isWhite == endPiece.isWhite) || "DS".contains(move.startName)) {
            return false;
        }

        if (move.startName.equals("K") && isLegalCastlingMove(move)) {
            return true;
        }

        if (!isBasicLegalMove(move) || !isUnobstructedMove(move)) {
            return false;
        }

        if (startPiece.name.equals("P") && !isLegalPawnMove(move)) {
            return false;
        }

        return forcedPieceMove(move);
    }

    /**
     * Compares the move vector with the known piece vectors. Castling is not considered a basic legal move.
     *
     * @param move move to be queried
     * @return whether it is a basic move or not
     */
    private boolean isBasicLegalMove(Move move) {
        int[][] moves_array;
        if (move.startName.equals("P") && move.isWhite) {
            moves_array = white_pawn_move;
        } else if (move.startName.equals("P")) {
            moves_array = black_pawn_move;
        } else if (move.startName.equals("R")) {
            moves_array = rook_move;
        } else if (move.startName.equals("N")) {
            moves_array = knight_move;
        } else if (move.startName.equals("B")) {
            moves_array = bishop_move;
        } else if (move.startName.equals("Q")) {
            moves_array = queen_move;
        } else {
            moves_array = king_move;
        }

        int[] vector = move.vector();
        for (int[] moveArr : moves_array) {
            if (vector[0] == moveArr[1] && vector[1] == moveArr[0]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the provided move is a castling move or not and if allowed to,
     * does the rook castle move automatically.
     *
     * @param move move to be queried
     * @return whether it is a legal castling move or not
     */
    private boolean isLegalCastlingMove(Move move) {
        if (gameState.castling.isEmpty() || Math.abs(move.xVector()) != 2 || move.yVector() != 0 || isInCheck(move)) {
            return false;
        }
        String castleType = move.xVector() > 0 ? "K" : "Q";
        castleType = move.isWhite ? castleType : castleType.toLowerCase();
        if (!gameState.castling.contains(castleType)) {
            return false;
        }
        Square[] threatSquares, pathSquares;
        int y = move.starty, x = move.startx;
        if (move.xVector() == 2) {
            threatSquares = new Square[]{board[y][x + 1], board[y][x + 2]};
            pathSquares = threatSquares;
        } else {
            threatSquares = new Square[]{board[y][x - 1], board[y][x - 2], board[y][x - 3]};
            pathSquares = new Square[]{board[y][x - 1], board[y][x - 2], board[y][x - 3]};
        }
        if (checkThreatsOn(threatSquares, move)) {
            return false;
        }
        for (Square square : pathSquares) {
            if (square.getOccupant() != null) {
                return false;
            }
        }
        if (move.real) {
            switch (castleType) {
                case "Q":
                    board[0][3].setOccupant(board[0][0].removeOccupant());
                    gameState = gameState.removeCastleRights("QK");
                    break;
                case "K":
                    board[0][5].setOccupant(board[0][7].removeOccupant());
                    gameState = gameState.removeCastleRights("QK");
                    break;
                case "q":
                    board[7][3].setOccupant(board[7][0].removeOccupant());
                    gameState = gameState.removeCastleRights("qk");
                    break;
                case "k":
                    board[7][5].setOccupant(board[7][7].removeOccupant());
                    gameState = gameState.removeCastleRights("qk");
                    break;
            }
        }
        return true;
    }

    /**
     * Checks if the given move is a legal pawn move or not and, if allowed to,
     * does enpassant pawn removals and promotions automatically
     *
     * @param move move to be queried
     * @return whether it is a legal pawn move or not
     */
    private boolean isLegalPawnMove(Move move) {
        Square[][] newBoard = move.real ? board : nextBoard;

        if (!move.startName.equals("P")) {
            return false;
        }

        if (move.xVector() == 0 && Math.abs(move.yVector()) == 1 && move.endPiece() == null) {
            if (move.real) {
                if (move.endy == 7 || move.endy == 0) {
                    board[move.starty][move.startx].setOccupant(new Piece("A", move.isWhite));
                }
            }
            return true;
        } else if (move.xVector() == 0 && Math.abs(move.yVector()) == 2 && move.endPiece() == null
                && (move.starty == 1 || move.starty == 6)) {
            if (move.startPiece().isWhite && newBoard[move.starty + 1][move.startx].getOccupant() == null) {
                if (move.real) {
                    gameState = gameState.updateEnpassant(board[move.endy - 1][move.endx]);
                }
                return true;
            } else if (!move.startPiece().isWhite && newBoard[move.starty - 1][move.startx].getOccupant() == null) {
                if (move.real) {
                    gameState = gameState.updateEnpassant(board[move.endy + 1][move.endx]);
                }
                return true;
            } else {
                return false;
            }
        } else if (Math.abs(move.xVector()) == 1 && Math.abs(move.yVector()) == 1) {
            if (move.endPiece() != null) {
                if (move.real) {
                    if (move.endy == 7 || move.endy == 0) {
                        board[move.starty][move.startx].setOccupant(new Piece("A", move.isWhite));
                    }
                }
                return true;
            } else if (gameState.enpassant == board[move.endy][move.endx]) {
                if ((gameState.enpassant.y == 2 && move.isWhite) || (gameState.enpassant.y == 5 && !move.isWhite)) {
                    return false;
                }
                if (move.real) {
                    int y = move.endy == 2 ? 3 : 4;
                    board[y][move.endx].removeOccupant();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the threats from the opposite colour on the list of squares provided.
     *
     * @param list list of squares to check sequentially
     * @param mainMove move to check for threats after
     * @return whether or not the list of squares provided are threatened by thr opposite colour
     */
    private boolean checkThreatsOn(Square[] list, Move mainMove) {
        Square[][] newBoard = mainMove.real ? board : nextBoard;
        for (Square end : list) {
            for (Square[] row : newBoard) {
                for (Square start : row) {
                    Move move = new Move(start.y, start.x, end.y, end.x);
                    boolean legal = isBasicLegalMove(move) && isUnobstructedMove(move) && (!move.startName.equals("P") || isLegalPawnMove(move));
                    if (start.getOccupant() != null && start.getOccupant().isWhite != mainMove.isWhite && legal) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets the position of the king of the given colour from the specified board
     *
     * @param move the move to check after
     * @return the square of the king piece
     */
    private Square getKingSquare(Move move) {
        Square[][] newBoard = move.real ? board : nextBoard;
        for (Square[] row : newBoard) {
            for (Square square : row) {
                if (!square.isEmpty() && "KCSD".contains(square.getName()) && square.isWhite() == move.isWhite) {
                    return square;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the king of a given colour is threatened on the specified board
     *
     * @param move the move to check after
     * @return whether or not the king is in check
     */
    private boolean isInCheck(Move move) {
        Square kingSquare = getKingSquare(move);
        if (kingSquare == null) {
            return false;
        }
        return checkThreatsOn(new Square[] {kingSquare}, move);
    }

    /**
     * Checks if spaces between the pieces in a given move are empty.
     * Always returns true if the starting pieces are pawns, kings or knights for obvious reasons.
     *
     * @param move move to be queried
     * @return whether or not the current move is obstructed
     */
    private boolean isUnobstructedMove(Move move) {
        Square[][] newBoard = move.real ? board : nextBoard;
        if ("QRB".contains(move.startName) && !move.startName.isEmpty()) {
            int endx = move.endx, endy = move.endy;
            while (endx != move.startx || endy != move.starty) {
                if (!newBoard[endy][endx].isEmpty() && (endx != move.endx || endy != move.endy))
                    return false;

                if (endx - move.startx != 0)
                    endx = endx - move.startx > 0 ? endx - 1 : endx + 1;

                if (endy - move.starty != 0)
                    endy = endy - move.starty > 0 ? endy - 1 : endy + 1;
            }
        }
        return true;
    }

    /**
     * Changes the positions of pieces on the board regardless of whether the move is legal or not.
     * The board used to move the pieces depends on whether the move is "real" or not. Can also be used
     * to check if the current move leads to a check after execution. Should only be called after the
     * move is confirmed to be legal.
     *
     * @param move move to be queried
     * @return whether or not the king is in check after the move is executed
     */
    private boolean forcedPieceMove(Move move) {
        if (move.real) {
            if (gameState.enpassant != null) {
                if ((gameState.enpassant.y == 2 && gameState.isWhite) || (gameState.enpassant.y == 5 && !gameState.isWhite)) {
                    gameState.updateEnpassant(null);
                }
            }
            nextBoard = cloneBoard();
            gameState = gameState.updateArrangement(getGameStateString());
            if (move.startName.equals("R")) {
                String castleRemove = "";
                if (move.startx == 0) {
                    castleRemove = "Q";
                } else if (move.startx == 7) {
                    castleRemove = "K";
                }
                castleRemove = move.isWhite ? castleRemove : castleRemove.toLowerCase();
                gameState = gameState.removeCastleRights(castleRemove);
            } else if ("KC".contains(move.startName)) {
                String castleRemove = move.isWhite ? "KQ" : "kq";
                isLegalCastlingMove(move);
                gameState = gameState.removeCastleRights(castleRemove);
            }
            isLegalPawnMove(move);
            gameState = gameState.toggleTurnWithIncrement();
        }
        Square[][] newBoard = move.real ? board : nextBoard;
        Piece moving = newBoard[move.starty][move.startx].removeOccupant();
        newBoard[move.endy][move.endx].setOccupant(moving);
        return !isInCheck(move);
    }

    /**
     * Returns a clone of the main board for the purpose of checking for threats without changing the original board.
     *
     * @return board clone
     */
    private Square[][] cloneBoard() {
        Square[][] array = new Square[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                array[i][j] = board[i][j].makeClone();
            }
        }
        return array;
    }

    /**
     * Converts the game arrangement into a string for fen format
     *
     * @return arrangement
     */
    private String getGameStateString() {
        StringBuilder state = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            int spaces = 0;
            for (Square square : board[i]) {
                Piece occupant = square.getOccupant();
                if (occupant != null) {
                    if (spaces > 0) {
                        state.append(spaces);
                        spaces = 0;
                    }
                    state = new StringBuilder(occupant.isWhite ? state + occupant.name : state + occupant.name.toLowerCase());
                } else {
                    spaces++;
                }
            }
            state = new StringBuilder(spaces > 0 ? state.toString() + spaces + "/" : state + "/");
        }
        return state.substring(0, state.length() - 1);
    }

    /**
     * Gets a particular square from the board given its normal name
     *
     * @param squareName square's normal name
     * @return specified square
     */
    private Square getSquare(String squareName) {
        if (squareName == null || squareName.length() != 2) {
            return null;
        }
        try {
            int x = squareName.charAt(0) - 'a', y = squareName.charAt(1) - '1';
            return board[y][x];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Replaces the promotable piece "A" with a new piece of choice. If the string is
     * of invalid format or if no piece is chosen a Queen is promoted automatically.
     *
     * @param newPieceName name of the promoted piece
     */
    public void replacePromotablePiece(String newPieceName) {
        Square promotionSquare = null;
        for (Square[] row: board) {
            for (Square square : row) {
                if (square.getName().equals("A")) {
                    promotionSquare = square;
                }
            }
        }

        if (promotionSquare == null) {
            return;
        }

        if ("BRK".contains(newPieceName) && newPieceName.length() == 1) {
            promotionSquare.setOccupant(new Piece(newPieceName));
        } else {
            promotionSquare.setOccupant(new Piece("Q", promotionSquare.isWhite()));
        }
    }

    /**
     * Checks if the current player is in checkmate, stalemate, or neither
     */
    private void switchKingWithMate() {
        boolean canMove = false;
        for (Square[] startRow : board) {
            for (Square start : startRow) {
                for (Square[] endRow : board) {
                    for (Square end : endRow) {
                        boolean legal = isLegalMove(new Move(start.y, start.x, end.y, end.x));
                        if (!legal) {
                            legal = isLegalMove(new Move(start.y, start.x, end.y, end.x));
                        }
                        if (start.getOccupant() != null && start.isWhite() == gameState.isWhite && legal) {
                            canMove = true;
                            break;
                        }
                    }
                }
            }
        }

        Move testMove = new Move(gameState.isWhite, true);
        Move inverseTestMove = new Move(!gameState.isWhite, true);

        String newKingName;
        if (isInCheck(testMove)) {
            newKingName = canMove ? "C" : "D";
        } else {
            newKingName = canMove ? "K" : "S";
        }

        Square kingSquare = getKingSquare(testMove);
        Square otherKingSquare = getKingSquare(inverseTestMove);

        if (kingSquare != null) {
            kingSquare.setOccupant(new Piece(newKingName, gameState.isWhite));
        }

        if (otherKingSquare != null && !otherKingSquare.getName().equals("K")) {
            otherKingSquare.setOccupant(new Piece("K", !gameState.isWhite));
        }
    }

    /**
     * Used to extract information from the board in a presentable format for GUI
     *
     * @return array of square contents
     */
    public String[] getPresentableArray() {
        String[] array = new String[64];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[7 - i][j].getOccupant() != null) {
                    array[(j * 8) + i] = board[7 - i][j].isWhite() ? "w" : "b";
                    array[(j * 8) + i] += board[7 - i][j].getName();
                } else {
                    array[(j * 8) + i] = "";
                }
            }
        }
        return array;
    }

    /**
     * Used to check if a given square is empty or not. Square is specified by it's normal name
     *
     * @param squareName square name in normal format
     * @return empty or not
     */
    public boolean squareIsEmpty(String squareName) {
        return getSquare(squareName) == null || getSquare(squareName).getOccupant() == null;
    }

    /**
     * Returns a list of all the squares reachable from the current square
     *
     * @param squareName square to start from in normal form
     * @return list of legally reachable squares
     */
    public String getValidMoves(String squareName) {
        Square start = getSquare(squareName);
        StringBuilder legalSquareNames = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boolean legal = isLegalMove(new Move(start.y, start.x, i, j));
                if (!legal) {
                    legal = isLegalMove(new Move(start.y, start.x, i, j));
                }
                if (legal) {
                    legalSquareNames.append(" ").append("abcdefgh".charAt(j)).append(i + 1);
                }
            }
        }

        return legalSquareNames.toString();
    }


    /**
     * The class of piece carries its name and colour. It is immutable as pieces cannot change type or colour.
     * The piece name should always be an uppercase single letter and the colour a boolean that is true when white.
     */
    private static class Piece {
        public final String name;
        public final boolean isWhite;

        public Piece(String pieceName) {
            name = ("" + pieceName.charAt(1)).toUpperCase();
            isWhite = pieceName.charAt(0) == 'w';
        }

        public Piece(String pieceName, boolean isWhite) {
            name = pieceName.toUpperCase();
            this.isWhite = isWhite;
        }
    }

    /**
     * The class of square carries a piece and information about its position in the board. Its coordinates
     * never change and the occupant can be set or removed. It has a few accessor methods for convenience and
     * a clone method that returns an identical square for board cloning.
     */
    private static class Square {
        public final int x, y;
        private Piece occupant;

        public Square(int y, int x) {
            this.x = x;
            this.y = y;
        }

        public Square(int y, int x, Piece occupant) {
            this(y, x);
            this.occupant = occupant;
        }

        public boolean isEmpty() {
            return occupant == null;
        }

        public String getName() {
            return occupant != null ? occupant.name : "";
        }

        public boolean isWhite() {
            return occupant != null && occupant.isWhite;
        }

        public Piece getOccupant() {
            return occupant;
        }

        public Piece removeOccupant() {
            Piece piece = occupant;
            occupant = null;
            return piece;
        }

        public void setOccupant(Piece occupant) {
            this.occupant = occupant;
        }

        public Square makeClone() {
            return new Square(y, x, occupant);
        }
    }

    /**
     * The class of Move carries information about a particular move on the board. It is immutable and can be "real"
     * or not. When a move is not "real" the actual repositioning of pieces never takes place on the main board.
     * When a move is real it is directly executed when checking for legality. A move should be made real only after
     * it has been first confirmed to be legal. It has a lot of final fields and accessor methods for convenience
     */
    private class Move {
        public final String startName;
        public final int startx, starty, endx, endy;
        public final boolean isWhite, real;

        public Move(int starty, int startx, int endy, int endx, boolean isReal) {
            this.startx = startx;
            this.starty = starty;
            this.endx = endx;
            this.endy = endy;
            isWhite = startPiece() == null || startPiece().isWhite;
            startName = startPiece() != null ? startPiece().name.toUpperCase() : "";
            real = isReal;
        }

        public Move(int starty, int startx, int endy, int endx) {
            this(starty, startx, endy, endx, false);
        }

        public Move(Move move, boolean isReal) {
            this.starty = move.starty;
            this.startx = move.startx;
            this.endy = move.endy;
            this.endx = move.endx;
            this.startName = move.startName;
            this.isWhite = move.isWhite;
            this.real = isReal;
        }

        public Move(boolean isWhite, boolean isReal) {
            this.isWhite = isWhite;
            real = isReal;
            startName = null;
            startx = starty = endx = endy = 0;
        }


        public int xVector() {
            return endx - startx;
        }

        public int yVector() {
            return endy - starty;
        }

        public int[] vector() {
            return new int[]{yVector(), xVector()};
        }

        public Piece startPiece() {
            Square[][] newBoard = real ? board : nextBoard;
            return newBoard[starty][startx].getOccupant();
        }

        public Piece endPiece() {
            Square[][] newBoard = real ? board : nextBoard;
            return newBoard[endy][endx].getOccupant();
        }

        public Move makeReal() {
            return real ? this : new Move(this, true);
        }
    }

    /**
     * The class of GameState carries information about the board and the current game. It is immutable and
     * made to be easily compatible with the fen format. When changing the game state the original state is not
     * changed but a new state is returned. This is so that game states can be easily stored in stacks.
     */
    private static class GameState {
        public final String arrangement, castling;
        public final Square enpassant;
        public final boolean isWhite;
        public final int fullMoves, halfMoves;

        public GameState(String arrangement, String castling, Square enpassant, boolean isWhite,
                         int halfMoves, int fullMoves) {
            this.arrangement = arrangement;
            this.castling = castling;
            this.enpassant = enpassant;
            this.isWhite = isWhite;
            this.halfMoves = halfMoves;
            this.fullMoves = fullMoves;
        }

        public GameState updateArrangement(String newArrangement) {
            return new GameState(newArrangement, castling, enpassant, isWhite, halfMoves, fullMoves);
        }

        public GameState removeCastleRights(String castleRights) {
            return new GameState(arrangement, castling.replaceAll(castleRights, ""), enpassant,
                    isWhite, halfMoves, fullMoves);
        }

        public GameState updateEnpassant(Square enpassantSquare) {
            return new GameState(arrangement, castling, enpassantSquare, isWhite, halfMoves, fullMoves);
        }

        public GameState toggleTurnWithIncrement() {
            return new GameState(arrangement, castling, enpassant, !isWhite, halfMoves + 1,
                    isWhite ? fullMoves : fullMoves + 1);
        }

        public GameState toggleTurn() {
            return new GameState(arrangement, castling, enpassant, !isWhite, halfMoves, fullMoves);
        }
    }
}
