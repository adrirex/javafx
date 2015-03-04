package games.sokoban.sokoban2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Sokoban implements ISokoban {
	
	private int width;
	private List<Cell> grid;

	public Sokoban() {
	}

	private void init(String[] lines) {
		// first we compute the width, as the maximum length of the lines
		width = 0;
		for (int y = 0; y < lines.length; y++) {
			width = Math.max(width, lines[y].length());
		}
		grid = new ArrayList<Cell>(lines.length * width);
		// fill the array with the characters in the lines
		// note that lines may be shorter than the width, so some cells may not be set
		for (int y = 0; y < lines.length; y++) {
			String line = lines[y];
			for (int x = 0; x < width; x++) {
				Cell cell = (x < line.length() ? new Cell(line.charAt(x)) : new Cell(' '));
				if (cell.isPlayer()) {
					playerX = x;
					playerY = y;
				}
				grid.add(cell);
			}
		}
	}
	
	public Sokoban(String[] lines) {
		init(lines);
	}
	
	@Override
	public String toString() {
		return toString("\n");
	}
	
	public String toString(String separator) {
		// with a StringBuilder, we can create a String without garbage
		StringBuilder builder = new StringBuilder();
		int num = 0;
		// for each cell
		for (Cell cell : grid) {
			// add the corresponding char
			builder.append(cell.toChar());
			num++;
			// if end-of-line and not end of grid, add separator
			if (num == width && num < grid.size()) {
				builder.append(separator);
				num = 0;
			}
		}
		// convert the StringBuilder to a String
		return builder.toString();
	}

	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return grid.size() / width;
	}

	public Cell getCell(int x, int y) {
		if (y < 0 || x < 0 || y >= getHeight() || x >= getWidth()) {
			return null;
		}
		return grid.get(y * width + x);
	}

	@Override
	public int countCells(Integer cellStatic, Integer cellDynamic) {
		int count = 0;
		// iterate over all cells
		for (Cell cell : grid) {
			if ((cellStatic == null || cellStatic.intValue() == cell.getStaticCellValue()) &&
				(cellDynamic == null || cellDynamic.intValue() == cell.getDynamicCellValue())) {
				count++;
			}
		}
		return count;
	}

	private int playerX = -1, playerY = -1;

	private Boolean doMove(int dx, int dy) {
		// a legal move is 1 in one direction and 0 in the other
		int x = playerX, y = playerY;
		boolean isPush = false;
		// if we need to move a box, i.e. overwrite the target, whether goal or floor
		Cell cell = getCell(x, y);
		Cell forward1 = getCell(x + dx, y + dy);
		Cell forward2 = getCell(x + dx + dx, y + dy + dy);
		if (forward1.isBox() && (! forward2.isOccupied())) {
			// remember that the move is a push
			isPush = true;
			forward1.moveTo(forward2);
		}
		if (isPush || (! forward1.isOccupied())) {
			// then move the player, i.e. overwrite the target, whether box (just moved), goal or floor
			cell.moveTo(forward1);
			playerX += dx;
			playerY += dy;
			return isPush;
		}
		return null;
	}

	private void undoMove(int dx, int dy, boolean wasPush) {
		int x = playerX, y = playerY;
		Cell cell = getCell(x, y);
		Cell backward = getCell(x - dx, y - dy);
		cell.moveTo(backward);
		if (wasPush) {
			Cell forward = getCell(x + dx, y + dy);
			forward.moveTo(cell);
		}
		playerX -= dx;
		playerY -= dy;
	}
	
	// ISokoban
	
	@Override
	public int getStaticCellValue(int x, int y) {
		return getCell(x, y).getStaticCellValue();
	}

	@Override
	public int getDynamicCellValue(int x, int y) {
		return getCell(x, y).getDynamicCellValue();
	}

	@Override
	public void load(InputStream inputStream) throws IOException {
		Collection<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith(";")) {
				continue;
			}
			lines.addAll(Arrays.asList(line.split("[\n\\|]+")));
			lines.remove(null);
		}
		reader.close();
		init(lines.toArray(new String[lines.size()]));
	}
	
	@Override
	public void save(OutputStream outputStream) throws IOException {
		PrintWriter writer = new PrintWriter(outputStream);
		writer.print(this);
		writer.flush();
	}

	public void init(String level) {
		init(level.split("[\n\\|]+"));
	}

	//
	
	private List<Move> moves = new ArrayList<Move>();
	private int undoPos = 0;
	
	@Override
	public Move movePlayer(int dx, int dy) {
		Boolean isPush = doMove(dx, dy);
		if (isPush != null) {
			Move moveCommand = new Move(dx, dy, isPush);
			while (moves.size() > undoPos) {
				moves.remove(moves.size() - 1);
			}
			moves.add(moveCommand);
			undoPos++;
			return moveCommand;
		}
		return null;
	}
	
	@Override
	public boolean canUndo() {
		return undoPos > 0;
	}
	
	@Override
	public void undo() {
		Move moveCommand = null;
		if (canUndo()) {
			undoPos--;
			moveCommand = moves.get(undoPos);
			undoMove(moveCommand.dx, moveCommand.dy, moveCommand.isPush);
		}
	}

	@Override
	public boolean canRedo() {
		return undoPos < moves.size();
	}
	
	@Override
	public void redo() {
		if (canRedo()) {
			Move moveCommand = moves.get(undoPos);
			undoPos++;
			doMove(moveCommand.dx, moveCommand.dy);
		}
	}
}
