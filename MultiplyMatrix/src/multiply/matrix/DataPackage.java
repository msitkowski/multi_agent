package multiply.matrix;

import java.util.ArrayList;
import java.util.List;

public class DataPackage {

    private int rowIndex, colIndex, result;
    private List<Integer> rowData, colData;

    public DataPackage() {

    }

    public DataPackage(int rId, int cId) {
        this.setRowIndex(rId);
        this.setColIndex(cId);
        this.setRowData(new ArrayList<Integer>());
        this.setColData(new ArrayList<Integer>());
        this.setResult(0);
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
    }

    public List<Integer> getRowData() {
        return rowData;
    }

    public void setRowData(List<Integer> rowData) {
        this.rowData = rowData;
    }

    public List<Integer> getColData() {
        return colData;
    }

    public void setColData(List<Integer> colData) {
        this.colData = colData;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
