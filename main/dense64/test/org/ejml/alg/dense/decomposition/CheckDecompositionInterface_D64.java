/*
 * Copyright (c) 2009-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.alg.dense.decomposition;

import org.ejml.data.DenseMatrix64F;
import org.ejml.interfaces.decomposition.DecompositionInterface;
import org.ejml.ops.MatrixFeatures_D64;
import org.ejml.ops.RandomMatrices_D64;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class CheckDecompositionInterface_D64 {

    /**
     * Performs a decomposition and makes sure the input matrix is not modified.
     */
    public static boolean safeDecomposition( DecompositionInterface<DenseMatrix64F> decomp , DenseMatrix64F A ) {

        DenseMatrix64F A_orig = decomp.inputModified() ? A.copy() : A;

        return decomp.decompose(A_orig);
    }


    /**
     * Checks to see if the matrix is or is not modified as according to the modified
     * flag.
     *
     * @param decomp
     */
    public static void checkModifiedInput( DecompositionInterface<DenseMatrix64F> decomp ) {
        DenseMatrix64F A = RandomMatrices_D64.createSymmPosDef(4,new Random(0x434));
        DenseMatrix64F A_orig = A.copy();

        assertTrue(decomp.decompose(A));

        boolean modified = !MatrixFeatures_D64.isEquals(A,A_orig);

        assertTrue(modified+" "+decomp.inputModified(),decomp.inputModified()==modified);
    }
}