/*
 * Copyright (c) 2009-2010, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * EJML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * EJML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EJML.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ejml.alg.block.linsol.chol;

import org.ejml.alg.block.BlockMatrixOps;
import org.ejml.alg.block.BlockTriangularSolver;
import org.ejml.alg.block.decomposition.chol.BlockCholeskyOuterForm;
import org.ejml.alg.block.linsol.LinearSolverBlock;
import org.ejml.data.BlockMatrix64F;
import org.ejml.data.D1Submatrix64F;
import org.ejml.ops.SpecializedOps;


/**
 * <p> Linear solver that uses a block cholesky decomposition. </p>
 *
 * <p>
 * Solver works by using the standard Cholesky solving strategy:<br>
 * A=L*L<sup>T</sup> <br>
 * A*x=b<br>
 * L*L<sup>T</sup>*x = b <br>
 * L*y = b<br>
 * L<sup>T</sup>*x = y<br>
 * x = L<sup>-T</sup>y
 * </p>
 *
 * <p>
 * It is also possible to use the upper triangular cholesky decomposition.
 * </p>
 *
 * @author Peter Abeles
 */
public class BlockCholeskyOuterSolver implements LinearSolverBlock {

    // cholesky decomposition
    private BlockCholeskyOuterForm chol = new BlockCholeskyOuterForm(true);

    // size of a block take from input matrix
    private int blockLength;

    // can the input matrix B in solve be overwritten.
    private boolean overwriteB = false;
    // temporary data structure used in some calculation.
    private double temp[];

    public void setOverwriteB( boolean doit ){
        this.overwriteB = doit;
    }

    @Override
    public BlockMatrix64F getA() {
        return chol.getT();
    }

    /**
     * Decomposes and overwrites the input matrix.
     *
     * @param A Semi-Positive Definite (SPD) system matrix. Modified. Reference saved.
     * @return If the matrix can be decomposed.  Will always return false of not SPD.
     */
    @Override
    public boolean setA(BlockMatrix64F A) {
        // Extract a lower triangular solution
        if( !chol.decompose(A) )
            return false;

        blockLength = A.blockLength;

        return true;
    }

    @Override
    public double quality() {
        return SpecializedOps.qualityTriangular(false,chol.getT());
    }

    @Override
    public void solve(BlockMatrix64F B, BlockMatrix64F X) {
        if( B.blockLength != blockLength )
            throw new IllegalArgumentException("Unexpected blocklength in B.");

        D1Submatrix64F L = new D1Submatrix64F(chol.getT());

        if( X != null ) {
            if( X.blockLength != blockLength )
                throw new IllegalArgumentException("Unexpected blocklength in X.");
            if( X.numRows != L.col1 ) throw new IllegalArgumentException("Not enough rows in X");
        } else if( !overwriteB ) {
            throw new IllegalArgumentException("X is null and overwriteB is false.");
        }
        if( B.numRows != L.col1 ) throw new IllegalArgumentException("Not enough rows in B");

        // need to set X to be since the solver overwrites the input matrix
        if( overwriteB ){
            X = B;
        } else {
            X.set(B);
        }

        //  L * L^T*X = B

        // Solve for Y:  L*Y = B
        BlockTriangularSolver.solve(blockLength,false,L,new D1Submatrix64F(X),false);

        // L^T * X = Y
        BlockTriangularSolver.solve(blockLength,false,L,new D1Submatrix64F(X),true);

    }

    @Override
    public void invert(BlockMatrix64F A_inv) {
        BlockMatrix64F T = chol.getT();
        if( A_inv.numRows != T.numRows || A_inv.numCols != T.numCols )
            throw new IllegalArgumentException("Unexpected number or rows and/or columns");


        if( temp == null || temp.length < blockLength*blockLength )
            temp = new double[ blockLength* blockLength ];

        // zero the upper triangular portion of A_inv
        BlockMatrixOps.zeroTriangle(true,A_inv);

        D1Submatrix64F L = new D1Submatrix64F(T);
        D1Submatrix64F B = new D1Submatrix64F(A_inv);

        // invert L from cholesky decomposition and write the solution into the lower
        // triangular portion of A_inv
        // B = inv(L)
        BlockTriangularSolver.invert(blockLength,false,L,B,temp);

        // B = L^-T * B
        // todo could speed up by taking advantage of B being lower triangular
        // todo take advantage of symmetry
        BlockTriangularSolver.solveL(blockLength,L,B,true);
    }

    @Override
    public boolean inputModified() {
        return chol.inputModified();
    }
}
