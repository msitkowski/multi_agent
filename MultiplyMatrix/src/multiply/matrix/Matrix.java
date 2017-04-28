package multiply.matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Matrix {

    private int rows;
    private int cols;

    private int[][] A;

    public Matrix(int r, int c) {

        this.rows = r;
        this.cols = c;
        this.A = new int[r][c];

        for (int i = 0; i < r; ++i) {
            for (int j = 0; j < c; ++j) {
                this.A[i][j] = 0;
            }
        }
    }

    public void generateValues() {
        Random r = new Random();

        for (int i = 0; i < this.rows; ++i) {
            for (int j = 0; j < this.cols; ++j) {
                this.A[i][j] = r.nextInt(10 - 1) + 1;
            }
        }
    }

    public Matrix multiplyMatrixes(Matrix M1, Matrix M2) {
        Matrix res = null;

        try {
            if (M1.rows == M2.cols) {
                res = new Matrix(M1.rows, M2.cols);

                for (int i = 0; i < M1.rows; ++i) {
                    for (int j = 0; j < M2.cols; ++j) {
                        for (int k = 0; k < M2.cols; ++k) {
                            res.A[i][j] += M1.A[i][k] * M2.A[k][j];
                        }
                    }
                }

            } else {
                // throw exception
                // Matrix 1 rows and Matrix 2 cols must be the same
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return res;
    }

    public List<Integer> getRowData(int rIndex) {
        List<Integer> row = new ArrayList<>();

        for (int i = 0; i < this.cols; ++i) {
            row.add(this.A[rIndex][i]);
        }

        return row;
    }

    public List<Integer> getColData(int cIndex) {
        List<Integer> col = new ArrayList<>();

        for (int i = 0; i < this.rows; ++i) {
            col.add(this.A[i][cIndex]);
        }

        return col;
    }

    public void displayMatrix() {
        for (int i = 0; i < this.rows; ++i) {
            for (int j = 0; j < this.cols; ++j) {
                System.out.print(this.A[i][j]);
                System.out.print(" ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public void setValue(int r, int c, int value) {
        try {
            this.A[r][c] = value;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
