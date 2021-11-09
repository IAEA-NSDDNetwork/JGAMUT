
package jgamut;

import ENSDF_data.*;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import ensdf_datapoint.dataPt;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import java.util.function.DoubleFunction;

/**
 * This class defines the methods which implements the gamma ray energy and intensity statistical procedures
 * used in the original GAMUT code.
 * 
 * Date Modified: 26/07/2015
 * 
 * @author Michael Birch
 */
public class GAMUT_Algorithms {
    
    /**
     * Copies the values of the elements of B into A
     * @param A the JAMA matrix to copy to
     * @param B the JAMA matrix to copy from
     */
    private static void copy(Matrix A, Matrix B){
        int n = Math.min(A.getColumnDimension(), B.getColumnDimension());
        int m = Math.min(A.getRowDimension(), B.getRowDimension());
        for(int j=0; j<n; j++){
            for(int i=0; i<m; i++){
                A.set(i, j, B.get(i, j));
            }
        }
    }
    
    /**
     * Returns a column vector with elements given by the ith row of M
     * @param M a JAMA Matrix
     * @param i the index of the row to take
     * @return a column vector with elements given by the ith row of M
     */
    private static Matrix getRow(Matrix M, int i){
        Matrix result = new Matrix(M.getColumnDimension(), 1);
        for(int j=0; j<result.getRowDimension(); j++){
            result.set(j, 0, M.get(i, j));
        }
        return result;
    }
    
    /**
     * Returns the outer product of the column vectors x and y.
     * @param x a column vector
     * @param y a column vector
     * @return the outer product of the column vectors x and y.
     */
    private static Matrix outerProduct(Matrix x, Matrix y){
        int N = x.getRowDimension();
        Matrix result = new Matrix(N, N);
        for(int i=0; i<N; i++){
            for(int j=0; j<N; j++){
                result.set(i, j, x.get(i, 0)*y.get(j, 0));
            }
        }
        return result;
    }
    
    /**
     * Sums the elements of M
     * @param M a JAMA Matrix
     * @return sum of the elements of M
     */
    private static double sum(Matrix M){
        double[] x = M.getColumnPackedCopy();
        
        return DoubleStream.of(x).sum();
    }
    
    /**
     * Returns the Moore–Penrose pseudoinverse, computed using singular 
     * value decomposition.
     * @param M the MAtrix to invert
     * @return the Moore–Penrose pseudoinverse, computed using singular 
     * value decomposition.
     */
    private static Matrix pseudoinverse(Matrix M){
        try{
            SingularValueDecomposition svd = M.svd();
            Matrix S, V, U;
            double tol = (1e-15)*Math.max(M.getColumnDimension(), M.getRowDimension());
            double s;
            int count;

            tol *= DoubleStream.of(svd.getSingularValues()).max().getAsDouble();
            S = svd.getS();
            V = svd.getV();
            U = svd.getU();

            count = 0;
            for(int i=0; i<S.getRowDimension(); i++){
                s = S.get(i, i);
                if(s > tol){
                    S.set(i, i, 1.0d/s);
                    count += 1;
                }
            }
            //count is the rank. Rank smaller than number column of columns
            //indicates that the system is under determined.
            if(count < M.getColumnDimension()){
                JOptionPane.showMessageDialog(null, "Warning! Linear system representing the level scheme is singular. A solution will still be obtained, however the result may not be reliable and the intermediate file should be checked for errors.\n\nNote: if energy shifts are being fitted and some datasets do not have gamma-ray energy data then that is likely the reason for this message.");
            }
            return V.times(S.times(U.transpose()));
        }catch(ArrayIndexOutOfBoundsException e){
            return M;
        }
    }
    
    /**
     * Sets up the linear system to be solved in the GAMUT gamma ray energy
     * algorithm.
     * @param data the dataset generated from the intermediate file. Contains
     * all of the experimental gamma rays with the levels already reduced 
     * to only the unique ones (i.e. levels which were the same from different
     * datasets were combined into a single level)
     * @param E the (JAMA) column vector to be filled with the experimental
     * gamma ray energies.
     * @param W the (JAMA) column vector to be filled with the weights for
     * the gamma ray energy measurements (1/uncertainty^2)
     * @param nonNumericDefaultUncertainty the (percent) uncertainty to assign
     * measurements with a non-numeric uncertainty (i.e. LT, GT, AP, CA).
     * Should be a number between 0 and 1.
     * @param useShifts set to <code>true</code> if the zero energy between
     * different datasets should be allowed to vary independently. 
     * @return the placement matrix G (matrix for the level scheme)
     */
    public static final Matrix createPlacementMatrix(ENSDFDataset data, 
            Matrix E, Matrix W, double nonNumericDefaultUncertainty,
            boolean useShifts){
        gamma[] gammas = data.getNonzeroGammas();
        level[] levels = data.getLevelsWithGammas();
        String[] sources = data.getSources();
        double[] G; //placement matrix stored in column-major order
        int n, m; //number of rows and columns in the matrix
        int i, j, k, count;
        String s;
        gamma g;
        double w;
        level p, f;
        int parentIndex, finalIndex;
        
        n = gammas.length - data.getNumAdoptedGammas() + data.getNumZeroLevels();
        if(useShifts){
            m = levels.length + sources.length;
        }else{
            m = levels.length;
        }
        
        //not enough information to determine dataset level shifts if
        //there are more columns than rows
        if(m > n && useShifts){
            return createPlacementMatrix(data, E, W, 
                    nonNumericDefaultUncertainty, false);
        }
        
        G = new double[n*m];
        
        for(i=0; i<G.length; i++){ //initialize all elements to 0
            G[i] = 0.0d;
        }
        for(i=0; i<n; i++){
            E.set(i, 0, 0.0d);
            W.set(i, 0, 0.0d);
        }
        
        count = 0;
        parentIndex = 0;
        finalIndex = 0;
        for(i=0; i<sources.length; i++){ //build matrix by dataset
            s = sources[i];
            for(j=0; j<gammas.length; j++){ //loop over gammas
                g = gammas[j];
                if(g.getSource().equals(s)){ //use only gammas from the current dataset
                    p = g.getParent();
                    f = g.getFinalLevel();
                    if(p == null || f == null){ //cannot be part of the matrix if parent/final levels not known
                        continue;
                    }
                    E.set(count, 0, g.getCMEnergy().toDouble());
                    try{
                        w = dataPt.constructFromString(g.getEg() + " " + g.getDEg()).getLower();
                    }catch(NullPointerException e){
                        w = nonNumericDefaultUncertainty; //use supplied absolute uncertainty
                    }
                    //System.out.println(w);
                    W.set(count, 0, 1.0d/(w*w));
                    for(k=0; k<levels.length; k++){ //loop over levels to find parent/final indecies
                        if(p.equals(levels[k])){
                            parentIndex = k;
                        }else if(f.equals(levels[k])){
                            finalIndex = k;
                        }
                    }
                    G[count + parentIndex*n] = 1.0d;
                    G[count + finalIndex*n] = -1.0d;
                    if(useShifts){
                        G[count + n*(levels.length + i)] = -1.0d;
                    }
                    count += 1;
                }
            }
        }
        //add zero level constraints
        count = n - data.getNumZeroLevels();
        w = 100.0d*W.norm1();
        for(i=0; i<levels.length; i++){
            if(levels[i].getEnergy().toDouble() < 1e-10){
                G[count + n*i] = 1.0d; //set level energy to zero
                W.set(count, 0, w); //Weight this fact with 100 times the sum of the other weights.
                //note that this is arbitrary
                count += 1;
            }
        }
        
        return new Matrix(G, n);
    }
    
    /**
     * Solves the linear least squares problem to fit the observed gamma ray
     * energies into the level scheme.
     * @param data the dataset from which G, E and W were generated
     * @param G the placement matrix
     * @param E the column vector of gamma energies 
     * @param W the column vector of weights (reciprocal of energy uncertainties
     * squared. Represents the diagonal of the weight matrix
     * @param fittedEg the column vector to fill with fitted gamma ray energies
     * @param dFittedEg the column vector to fill with uncertainties for fitted energies
     * @param dFit the column vector of uncertainties in the fitted quantities
     * @return the fitted level energies (and shifts, if those were included in G)
     */
    public static final Matrix solveLevelSchemeSystem(ENSDFDataset data,
            Matrix G, Matrix E, Matrix W, Matrix fittedEg, Matrix dFittedEg,
            Matrix dFit){
        
        Matrix V; //variance matrix
        Matrix WG, WE;
        Matrix solution;
        
        //only the level scheme parts of G (no zero level constraints or 
        //energy shifts)
        Matrix lvlScheme = new Matrix(G.getRowDimension(), G.getColumnDimension());
        
        double sigma;
        int i, j, m, n;
        
        WG = new Matrix(G.getRowDimension(), G.getColumnDimension());
        for(j=0; j<WG.getColumnDimension(); j++){
            for(i=0; i<WG.getRowDimension(); i++){
                WG.set(i, j, W.get(i, 0)*G.get(i, j));
            }
        }
        WE = new Matrix(E.getRowDimension(), 1);
        for(i=0; i<WE.getRowDimension(); i++){
            WE.set(i, 0, W.get(i, 0)*E.get(i, 0));
        }
        
        V = pseudoinverse(((G.transpose()).times(WG)));
        //V = ((G.transpose()).times(WG)).inverse();
        
        solution = V.times(G.transpose().times(WE));
        
        m = data.getNumLevelsWithGammas();
        n = data.getNumUniqueGammas();
        sigma = Math.sqrt((E.transpose().times(WE)).minus(E.transpose().times(WG.times(solution))).get(0,0)/
                (double)(n-m));
        
        for(i=0; i<dFit.getRowDimension(); i++){
            dFit.set(i, 0, sigma*Math.sqrt(V.get(i,i)));
        }
        
        copy(lvlScheme, G.getMatrix(0, data.getNumNonzeroGammas() - 
                data.getNumAdoptedGammas() - 1, 0, m-1));

        copy(fittedEg, lvlScheme.times(solution));
        for(i=0; i<dFittedEg.getRowDimension(); i++){
            dFittedEg.set(i, 0, sigma*Math.sqrt(sum(outerProduct(getRow(lvlScheme, i),
                    getRow(lvlScheme, i)).arrayTimes(V))));
        }
        
        
        return solution;
    }
    
    /**
     * Returns <code>true</code> if the given String can be parsed into an
     * integer.
     * @param s the given String
     * @return <code>true</code> if the given String can be parsed into an
     * integer.
     */
    private static boolean isInt(String s){
        try{
            Integer.parseInt(s);
            return true;
        }catch(NumberFormatException e){
            return false;
        }
    }
    
    /**
     * Asks the user to change the uncertainty in the energy of the specified
     * gamma-ray, which has the specified parent level, to the specified new
     * uncertainty.
     * @param g the specified gamma-ray
     * @param p the initial level of the specified gamma-ray
     * @param newSigma the new uncertainty of the gamma-ray energy if the user
     * agrees.
     * @return <code>true</code> if the user agrees to the uncertainty change
     */
    private static boolean askChangeEnergyUncert(gamma g, level p, double newSigma){
        String title, message, newUncert;
        String answer;
        List<gammaEquivalenceClass> G;
        List<String> context;
        int[] lineNumber = new int[1];
        gammaContextDialog contextWindow = new gammaContextDialog(null, false);
        JOptionPane questionPane;
        JDialog questionDialog;
        
        title = "Change Uncertainty?";
        newUncert = String.format("%1.3f", newSigma); // dataPt.ENSDFprint(g.getEg(), String.valueOf(newSigma), false);
        message = "Gamma ray with energy " + g.getEg() + " " + g.getDEg() + 
                " with parent level " + p.getLvlRecord().getE() + "  " + 
                p.getLvlRecord().getJ() + " from data set " + g.getSource() + 
                " differs from fitted energy by three or more standard deviations. Increase the uncertainty to " + 
                newUncert + "?";
        
        G = ENSDFDataset.groupByGamma(p);
        context = ENSDFDataset.generateMiniDatasetTable(G, lineNumber, g);
        contextWindow.setTextArea(context);
        contextWindow.highlightLine(lineNumber[0]);
        contextWindow.setVisible(true);
        
        questionPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        questionDialog = questionPane.createDialog(title);
        questionDialog.setModal(false);
        questionDialog.setVisible(true);
        
        answer = String.valueOf(questionPane.getValue());
        while(!isInt(answer)){
            answer = String.valueOf(questionPane.getValue());
            if(answer.equals("null")){ //question dialog was closed by user
                answer = String.valueOf(JOptionPane.NO_OPTION);
            }
        }
        
        contextWindow.setVisible(false);
        contextWindow.dispose();
        return Integer.parseInt(answer) == JOptionPane.YES_OPTION;
    }
    
    /**
     * Asks the user to change the uncertainty in the intensity of the specified
     * gamma-ray, which has the specified parent level, to the specified new
     * uncertainty.
     * @param g the specified gamma-ray
     * @param p the initial level of the specified gamma-ray
     * @param newSigma the new uncertainty of the gamma-ray intensity if the user
     * agrees.
     * @return <code>true</code> if the user agrees to the uncertainty change
     */
    private static boolean askChangeIntensityUncert(gamma g, level p, double newSigma){
        String title, message, newUncert;
        String answer;
        List<gammaEquivalenceClass> G;
        List<String> context;
        int[] lineNumber = new int[1];
        gammaContextDialog contextWindow = new gammaContextDialog(null, false);
        JOptionPane questionPane;
        JDialog questionDialog;
        
        title = "Change Uncertainty?";
        newUncert = String.format("%1.3f", newSigma); //dataPt.ENSDFprint(g.getEg(), String.valueOf(newSigma), false);
        message = "Gamma ray with energy " + g.getEg() + " " + g.getDEg() + 
                " and intensity " + g.getIg() + " " + g.getDIg() + 
                " with parent level " + p.getLvlRecord().getE() + "  " + 
                p.getLvlRecord().getJ() + " from data set " + g.getSource() + 
                " differs from fitted intensity by four or more standard deviations. Increase the uncertainty to " + 
                newUncert + "?";
        
        G = ENSDFDataset.groupByGamma(p);
        context = ENSDFDataset.generateMiniDatasetTable(G, lineNumber, g);
        contextWindow.setTextArea(context);
        contextWindow.highlightLine(lineNumber[0]);
        contextWindow.setVisible(true);
        
        questionPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        questionDialog = questionPane.createDialog(title);
        questionDialog.setModal(false);
        questionDialog.setVisible(true);
        
        answer = String.valueOf(questionPane.getValue());
        while(!isInt(answer)){
            answer = String.valueOf(questionPane.getValue());
            if(answer.equals("null")){ //question dialog was closed by user
                answer = String.valueOf(JOptionPane.NO_OPTION);
            }
        }
        
        contextWindow.setVisible(false);
        contextWindow.dispose();
        return Integer.parseInt(answer) == JOptionPane.YES_OPTION;
    }
    
    /**
     * Asks the user to energy a new uncertainty for the given gamma-ray
     * @param g the given gamma-ray
     * @return <code>true</code> if the user agrees to enter a new uncertainty
     */
    private static boolean askEnterUncert(gamma g){
        String title, message;
        int answer;
        
        title = "Change Uncertainty?";
        message = "Would you like to enter your own new uncertainty for " + 
                g.getEg() + " " + g.getDEg() + "?";
        answer = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }
    
    /**
     * Creates an input window for the user to enter a new uncertainty (not in
     * ENSDF format).
     * @return the input uncertainty from the user
     */
    private static double getUserUncert(){
        String title, message;
        String answer;
        
        title = "Enter new uncertainty";
        message = "New uncertainty (raw value, i.e. not in ENSDF format): ";
        answer = JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
        try{
            return Double.parseDouble(answer);
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null, "Error! Input a numeric value for the uncertainty!", 
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return getUserUncert();
        }catch(NullPointerException e){
            return -1.0d;
        }
    }
    
    /**
     * Returns the gamma with the largest chi^2 when comparing its energy to the
     * fitted energy.
     * @param data the data which was used to obtain the fit
     * @param fitEg the fitted gamma-ray energies
     * @param W the weights of the gamma-rays
     * @param index the zeroth element of this array is assigned the index of the
     * returned gamma
     * @param _chiSq the zeroth element of this array is assigned the value of 
     * the largest chi^2.
     * @return the gamma with the largest chi^2 when comparing its energy to the
     * fitted energy
     */
    public static gamma largestEgChiSq(ENSDFDataset data, Matrix fitEg,
            Matrix W, int[] index, double[] _chiSq){
        gamma[] gammas = data.getNonzeroGammas();
        String[] sources = data.getSources();
        int count, i, j;
        gamma g;
        level p, f;
        String s;
        double E, chiSq, maxChiSq;
        gamma result;
        
        maxChiSq = 0.0d;
        result = null;
        count = 0;
        for(i=0; i<sources.length; i++){
            s = sources[i];
            for(j=0; j<gammas.length; j++){
                g = gammas[j];
                if(g.getSource().equals(s)){
                    p = g.getParent();
                    f = g.getFinalLevel();
                    if(p == null || f == null){
                        continue;
                    }
                    E = g.getCMEnergy().toDouble();
                    chiSq = W.get(count, 0)*(E - fitEg.get(count, 0))*(E - fitEg.get(count, 0));
                    if(chiSq > maxChiSq){
                        result = g;
                        maxChiSq = chiSq;
                        index[0] = count;
                    }
                    count += 1;
                }
            }
        }
        _chiSq[0] = maxChiSq;
        return result;
    }
    
    /**
     * Performs a Chi Squared analysis on the fitted gamma ray energies. The user
     * is asked to increase the uncertainty of gamma rays which are differ from
     * the fit by 3 or more standard deviations.
     * @param data dataset on which the fit was performed
     * @param G the placement matrix (produced by 
     * {@link #createPlacementMatrix(ENSDF_data.ENSDFDataset, Jama.Matrix, Jama.Matrix, double, boolean)})
     * @param L the fitted level energies and energy shifts (result of 
     * {@link #solveLevelSchemeSystem(ENSDF_data.ENSDFDataset, Jama.Matrix, Jama.Matrix, Jama.Matrix, Jama.Matrix, Jama.Matrix, Jama.Matrix)})
     * @param W matrix of weights (i.e. reciprocal squared uncertainties),
     * elements of this matrix will be adjusted as the uncertainties are changed.
     * @return <code>true</code> when no gamma ray energy uncertainties were
     * adjusted, <code>false</code> otherwise.
     */
    public static boolean EgChiSqAnalysis(ENSDFDataset data, Matrix G, Matrix L, Matrix W){
        int count;
        gamma g;
        level p;
        double E, chiSq, W_new, sigma_new;
        int[] index = new int[1];
        double[] _chiSq = new double[1];
        boolean result;
        Matrix fitEg = G.times(L);
        
        result = true;
        g = largestEgChiSq(data, fitEg, W, index, _chiSq);
        count = index[0];
        chiSq = _chiSq[0];
        try{
            E = g.getCMEnergy().toDouble();
        }catch(NullPointerException e){
            E = fitEg.get(count, 0);
        }
        
        if(Math.sqrt(chiSq) > 3){
            sigma_new = Math.abs(E - fitEg.get(count, 0));
            W_new = 1.0d/(sigma_new*sigma_new);
            p = g.getParent();
            if(askChangeEnergyUncert(g, p, sigma_new)){
                W.set(count, 0, W_new);
                g.setNumericDEg(sigma_new);
                result = false;
                return result;
            }else if(askEnterUncert(g)){
                sigma_new = getUserUncert();
                if(sigma_new > 0.0d){
                    W_new = 1.0d/(sigma_new*sigma_new);
                    W.set(count, 0, W_new);
                    g.setNumericDEg(sigma_new);
                    result = false;
                }
                return result;
            }
        }
        return result;
    }
    
    /**
     * Fills the matrices I and W with gamma ray intensities and their reciprocal
     * uncertainties squared.
     * @param data the dataset containing the gamma ray data
     * @param I the matrix of intensities
     * @param W the matrix if reciprocal square uncertainties
     * @param nonNumericDefaultUncertainty the (percent) uncertainty to assign
     * gamma ray intensities with non-numeric uncertainty (including those with
     * missing uncertainty)
     */
    public static void setupIntensityMatrices(ENSDFDataset data, Matrix I, 
            Matrix W, double nonNumericDefaultUncertainty){
        
        level[] levels = data.getLevelsWithGammas();
        List<String> sources = java.util.Arrays.asList(data.getSources());
        List<gammaEquivalenceClass> tmp;
        int k, i, j;
        double w;
        
        i = 0;
        for(k=0; k<levels.length; k++){
            tmp = ENSDFDataset.groupByGamma(levels[k]);
            for(gammaEquivalenceClass G : tmp){
                for(gamma g : G.getGammas()){
                    j = sources.indexOf(g.getSource());
                    if(j == -1){
                        continue; //source not in list, i.e. must be from adopted
                    }
                    //if the normalized intensity has no uncertainty then
                    //either the original had no uncertainty or there is 
                    //only 1 gamma de-exciting the level, so the 
                    //normalized intensity is 100 by definition. In
                    //either case we should ignore it.
                    if (!g.getDIg(true).equals("") && g.ig.isPresent()) {
                        I.set(i, j, g.ig.get());
                        try{
                            w = dataPt.constructFromString(g.getIg() + " " + g.getDIg()).getLower();
                        }catch(NullPointerException e){
                            w = nonNumericDefaultUncertainty*I.get(i,j);
                        }
                        W.set(i, j, 1.0/(w*w));
                    } else {
                        I.set(i, j, -1.0d);
                        W.set(i, j, 1.0d);
                    }
                }
                if(!G.isEmpty()){
                    for(gamma g : G.getGammas()){
                        if(!g.isAdopted()){
                            i += 1;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Calculates the intensity weighted averages using the given weights and 
     * scale factors (to put everything on the same scale).
     * @param I the intensities to average (see {@link #setupIntensityMatrices(ENSDF_data.ENSDFDataset, Jama.Matrix, Jama.Matrix, double) 
     * setupIntensityMatrices}).
     * @param W the weights of the intensities
     * @param beta the scale factors to put all intensities on the same scale
     * @param dIbar if not <code>null</code> then the uncertainties of the 
     * weighted averages are recorded in this vector.
     * @return the intensity weighted averages using the given weights and 
     * scale factors.
     */
    public static Matrix calcIbar(Matrix I, Matrix W, Matrix beta, Matrix dIbar){
        Matrix Ibar = new Matrix(I.getRowDimension(), 1);
        double sum_num, sum_denom;
        int i, j;
        
        for(i=0; i<Ibar.getRowDimension(); i++){
            sum_num = 0.0d;
            sum_denom = 0.0d;
            for(j=0; j<beta.getRowDimension(); j++){
                if(I.get(i, j) > 0.0d){
                    sum_num += W.get(i, j)*I.get(i, j)*beta.get(j, 0);
                    sum_denom += W.get(i, j)*beta.get(j, 0)*beta.get(j, 0);
                }
            }
            if(sum_num <= 0.0d){
                Ibar.set(i, 0, -1.0d);
            }else{
                Ibar.set(i, 0, sum_num/sum_denom);
            }
            if(dIbar != null){
                if(sum_denom > 0.0d){
                    dIbar.set(i, 0, 1.0d/Math.sqrt(sum_denom));
                }else{
                    dIbar.set(i, 0, -1.0d);
                }
            }
        }
        
        return Ibar;
    }
    /**
     * Calls <code>{@link #calcIbar(Jama.Matrix, Jama.Matrix, Jama.Matrix, Jama.Matrix) 
     * calcIbar(I, W, beta, null)}</code>.
     * @param I the intensities to average (see {@link #setupIntensityMatrices(ENSDF_data.ENSDFDataset, Jama.Matrix, Jama.Matrix, double) 
     * setupIntensityMatrices}).
     * @param W the weights of the intensities
     * @param beta the scale factors to put all intensities on the same scale
     * @return the intensity weighted averages using the given weights and 
     * scale factors. 
     */
    public static Matrix calcIbar(Matrix I, Matrix W, Matrix beta){
        return calcIbar(I, W, beta, null);
    }
    
    /**
     * Calculated the Chi Squared for the intensities using the given value of
     * the intensity multipliers
     * @param I the experimental intensities
     * @param W the weights of the intensities
     * @param beta the intensity multipliers
     * @return the Chi Squared
     */
    public static double calcIntensityChiSq(Matrix I, Matrix W, Matrix beta){
        Matrix Ibar = calcIbar(I, W, beta);
        double sum = 0.0d;
        double r;
        int i, j;
        
        for(i=0; i<I.getRowDimension(); i++){
            for(j=0; j<I.getColumnDimension(); j++){
                if(I.get(i, j) > 0.0d){
                    r = I.get(i, j) - beta.get(j,0)*Ibar.get(i,0);
                    sum += W.get(i, j)*r*r;
                }
            }
        }
        
        return sum;
    }
    /**
     * Calculates the total chi^2 of the intensity fit using the given weights 
     * and scale factors. This method first changes the <code>j</code>-th 
     * scale factor to <code>x</code>.
     * @param I the intensities to average (see {@link #setupIntensityMatrices(ENSDF_data.ENSDFDataset, Jama.Matrix, Jama.Matrix, double) 
     * setupIntensityMatrices}).
     * @param W the weights of the intensities
     * @param beta the scale factors to put all intensities on the same scale
     * @param j the index of the scale factors to change
     * @param x the new value of the jth scale factor
     * @return the total chi^2 of the intensity fit using the given weights 
     * and scale factors
     */
    public static double calcIntensityChiSq(final Matrix I, final Matrix W, 
            Matrix beta, int j, double x){
        beta.set(j, 0, x);
        return calcIntensityChiSq(I, W, beta);
    }
    
    /**
     * Uses golden section search to find the minimum of f on [a,b].
     * @param f function to minimize
     * @param a left interval end point
     * @param b right interval end point
     * @param tol tolerance parameter
     * @return the value at which f is minimized
     */
    public static double goldenSearch(DoubleFunction<Double> f, double a, double b, double tol){
        final double gr = 0.5d*(Math.sqrt(5) - 1.0d); //golden ratio
        double c, d;
        
        c = b - gr*(b-a);
        d = a + gr*(b-a);
        while( Math.abs(c-d) > tol){
            if(f.apply(c) < f.apply(d)){
                b=d;
                d=c;
                c=b-gr*(b-a);
            }else if(f.apply(c) > f.apply(d)){
                a=c;
                c=d;
                d=a+gr*(b-a);
            }else{
                break;
            }
        }
        return 0.5d*(a+b);
    }
    
    /**
     * Performs a single iteration of the scheme to solve for the intensity 
     * multipliers, beta_j.
     * @param I Matrix of intensity measurements
     * @param W Matrix of intensity weights
     * @param beta column vector of intensity multipliers
     * @param normalizeIbar <code>true</code> if ensure that the sum of
     * the adopted intensities is 1.
     * @return the percent difference between the old and new beta vectors
     */
    private static double betaUpdate(Matrix I, Matrix W, Matrix beta, 
            boolean normalizeIbar){
        
        Matrix oldBeta = beta.copy();
        int i,j;
        double sum_num, sum_denom;
        
        if(normalizeIbar){
            //iterative solution method is stable when Ibar is normalized
            Matrix Ibar = calcIbar(I, W, beta);
            sum_num = 0.0d;
            for(i=0; i<Ibar.getRowDimension(); i++){
                if(Ibar.get(i, 0) > 0.0d){
                    sum_num += Ibar.get(i, 0);
                }
            }
            Ibar = Ibar.times(1.0d/sum_num); //normalize so sum (of valid elements) is 1
            for(j=0; j<beta.getRowDimension(); j++){
                sum_num = 0.0d;
                sum_denom = 0.0d;
                for(i=0; i<Ibar.getRowDimension(); i++){
                    if(I.get(i, j) > 0.0d){
                        sum_num += W.get(i, j)*I.get(i, j)*Ibar.get(i, 0);
                        sum_denom += W.get(i, j)*Ibar.get(i, 0)*Ibar.get(i, 0);
                    }
                }
                if(sum_num > 0.0d){
                    beta.set(j, 0, sum_num/sum_denom);
                }else{
                    beta.set(j, 0, -1.0d);
                }
            }
        }else{
            //golden search direct maximaization is more stable when 
            //Ibar is not normalized. Note: this is because there
            //is a symmetry in the global chi^2: Ibar -> Ibar*k; beta -> beta/k
            //for any k. However, when the sum of Ibar is forced to be 1 then
            //this symmetry is broken.
            for(j=1; j<beta.getRowDimension(); j++){
                final double a = Math.max(beta.get(j, 0) - 0.9, 0.00001d);
                final double b = beta.get(j, 0) + 0.9;
                final int j_final = j;
                beta.set(j, 0, goldenSearch(x -> {return calcIntensityChiSq(I, W, beta, j_final, x);}, a, b, 1e-5));
            }
        }  
        
        return beta.minus(oldBeta).normF() / oldBeta.normF();
    }
    
    /**
     * Calculates the intensity multipliers to put all measurements on the
     * same scale.
     * @param I the matrix of measured intensities
     * @param W the matrix of intensity weights
     * @param normalizeIbar <code>true</code> if ensure that the sum of
     * the adopted intensities is 1.
     * @return the column vector of intensity multipliers
     */
    public static Matrix intensitySolve(Matrix I, Matrix W, boolean normalizeIbar){
        Matrix beta = new Matrix(I.getColumnDimension(), 1, 1.0d);   
        double eps;
        if(normalizeIbar){
            eps = 1e-9;
        }else{
            eps = 1e-4;
        }
        
        while(betaUpdate(I, W, beta, normalizeIbar) > eps){}
        
        return beta;
    }
    
    /**
     * Returns the gamma with the largest chi^2 when comparing its intensity to the
     * fitted intensity.
     * @param data the data which was used to obtain the fit
     * @param beta the fitted scale factors
     * @param Ibar the fitted intensities
     * @param W the weights of the gamma-rays
     * @param indicies the first two elements of this array will be assigned
     * the indices in the intensity matrix for the returned gamma
     * @param _chiSq the zeroth element of this array is assigned the value of 
     * the largest chi^2.
     * @return the gamma with the largest chi^2 when comparing its intensity to the
     * fitted intensity
     */
    public static gamma largestIgChiSq(ENSDFDataset data, Matrix beta, Matrix Ibar,
            Matrix W, int[] indicies, double[] _chiSq){
        level[] levels = data.getLevelsWithGammas();
        List<String> sources = java.util.Arrays.asList(data.getSources());
        List<gammaEquivalenceClass> tmp;
        int k, i, j;
        double chiSq, I, maxChiSq;
        gamma result;
        
        i = 0;
        maxChiSq = 0.0d;
        result = null;
        for(k=0; k<levels.length; k++){
            tmp = ENSDFDataset.groupByGamma(levels[k]);
            for(gammaEquivalenceClass G : tmp){
                for(gamma g : G.getGammas()){
                    j = sources.indexOf(g.getSource());
                    if(j == -1){
                        continue; //source not in list, i.e. must be from adopted
                    }
                    if (!g.getDIg(true).equals("") && g.ig.isPresent()) {
                        I = g.ig.get();
                        chiSq = W.get(i, j)*(I - beta.get(j, 0)*Ibar.get(i, 0))
                                *(I - beta.get(j, 0)*Ibar.get(i, 0));
                        if(chiSq > maxChiSq){
                            maxChiSq = chiSq;
                            result = g;
                            indicies[0] = i;
                            indicies[1] = j;
                        }
                    }
                }
                if(!G.isEmpty()){
                    for(gamma g : G.getGammas()){
                        if(!g.isAdopted()){
                            i += 1;
                            break;
                        }
                    }
                }
            }
        }
        _chiSq[0] = maxChiSq;
        return result;
    }
    
    /**
     * Performs a Chi Squared analysis on the fitted gamma ray intensities. The user
     * is asked to increase the uncertainty of gamma rays which are differ from
     * the fit by 4 or more standard deviations.
     * @param data dataset on which the fit was performed
     * @param beta the fitted scale factors
     * @param Ibar the fitted intensities
     * @param W matrix of weights (i.e. reciprocal squared uncertainties),
     * elements of this matrix will be adjusted as the uncertainties are changed.
     * @return <code>true</code> when no gamma ray energy uncertainties were
     * adjusted, <code>false</code> otherwise.
     */
    public static boolean IgChiSqAnalysis(ENSDFDataset data, Matrix beta, Matrix Ibar,
            Matrix W){
        int i, j;
        double chiSq, I, sigma_new, W_new;
        double[] _chiSq = new double[1];
        level p;
        gamma g;
        int[] indicies = new int[2];
        boolean result;
        
        result = true;
        g = largestIgChiSq(data, beta, Ibar, W, indicies, _chiSq);
        i = indicies[0];
        j = indicies[1];
        if (g != null && g.ig.isPresent()) {
            I = g.ig.get();
            chiSq = _chiSq[0];
        } else {
            I = 0.0d;
            chiSq = 0.0d;
        }
        if(Math.sqrt(chiSq) > 4){
            sigma_new = Math.abs(I - beta.get(j, 0)*Ibar.get(i, 0));
            W_new = 1.0d/(sigma_new*sigma_new);
            p = g.getParent();
            if(askChangeIntensityUncert(g, p, sigma_new)){
                W.set(i, j, W_new);
                g.setNumericDIg(sigma_new);
                result = false;
            }else if(askEnterUncert(g)){
                try{
                    sigma_new = dataPt.constructFromString(g.getIg() + 
                            " " + String.valueOf(getUserUncert())).getUpper();
                    W_new = 1.0d/(sigma_new*sigma_new);
                    W.set(i, j, W_new);
                    g.setNumericDIg(sigma_new);
                    result = false;
                }catch(NullPointerException e){
                    //failed to change the uncertainty
                }

            }
        }
        return result;
    }
    
    /**
     * Uses the GAMUT intensity algorithm on each level to determine the 
     * adopted intensities for the gamma rays.
     * @param data the dataset containing the gamma ray data
     * @param Ibar the column vector which will be filled with the adopted
     * intensities
     * @param dIbar the column vector which will be filled with the uncertainties
     * on the adopted intensities
     * @param nonNumericDefaultUncertainty the (percent) uncertainty to assign
     * gamma ray intensities with non-numeric uncertainty (including those with
     * missing uncertainty) 
     */
    public static void calcIntensitiesLevelByLevel(ENSDFDataset data, Matrix Ibar,
            Matrix dIbar, double nonNumericDefaultUncertainty){
        int j, offset;
        level[] levels = data.getLevelsWithGammas();
        ENSDFDataset tmp;
        Matrix I, W, beta, Ibar_level, dIbar_level;
        offset = 0;
        gamma tmpLastGamma;
        for (level l : levels) {
            tmp = new ENSDFDataset(data.getNucid(), "tmp", "");
            tmp.addLevel(new level(l.getLvlRecord(), l.getSource()), false);
            for (gamma g : l.getGammaOutList()) {
                tmp.addGamma(new gamma(g.getGammaRecord(), g.getSource()), tmp.getLastLevel());
                tmpLastGamma = tmp.getLastGamma();
                tmpLastGamma.setFinalLevel(g.getFinalLevel());
                if (!tmpLastGamma.dig.isPresent()) {
                    if (tmpLastGamma.ig.isPresent()) {
                        tmpLastGamma.setNumericDIg(nonNumericDefaultUncertainty*tmpLastGamma.ig.get());
                    }
                }
            }
            if(tmp.getGammas().length == 0){
                continue;
            }
            tmp.renormalizeGammaRays(false);
            l.gammaSources = tmp.getSources();
            I = new Matrix(tmp.getNumUniqueGammas(), tmp.getNumSources());
            W = new Matrix(I.getRowDimension(), I.getColumnDimension());
            dIbar_level = new Matrix(I.getRowDimension(), 1);
            setupIntensityMatrices(tmp, I, W, nonNumericDefaultUncertainty);
            beta = intensitySolve(I, W, true);
            Ibar_level = calcIbar(I, W, beta, dIbar_level);
            while(!IgChiSqAnalysis(tmp, beta, Ibar_level, W)){
                beta = intensitySolve(I, W, true);
                Ibar_level = calcIbar(I, W, beta, dIbar_level);
            }
            l.beta = beta.getColumnPackedCopy();

            for(j=0; j<Ibar_level.getRowDimension(); j++){
                Ibar.set(offset+j, 0, Ibar_level.get(j, 0));
                dIbar.set(offset+j, 0, dIbar_level.get(j, 0));
            }
            offset += j;
        }
    }
    
    /**
     * Determines linear shifts in the gamma-ray energies of each measurement
     * (source) relative to a particular source (the "standard"). The calculated
     * shifts are of the form <code>E_i^j + m_j*E_i^s + b_j = E_i^s</code>, where
     * <code>E_i^j</code> is the energy of the <code>i</code>-th gamma-ray in the
     * <code>j</code>-th dataset, <code>E_i^s</code> is the energy of the
     * <code>i</code>-th gamma-ray in the standard dataset, <code>m_j</code> is
     * the slope of the linear shift for the <code>j</code>-th dataset, and
     * <code>b_j</code> is the intercept of the linear shift for the
     * <code>j</code>-th dataset.
     * @param data the dataset generated from the intermediate file. Contains
     * all of the experimental gamma rays with the levels already reduced 
     * to only the unique ones (i.e. levels which were the same from different
     * datasets were combined into a single level)
     * @param standard the title of the dataset (the source) to be used as 
     * the standard all other datasets are compared against.
     * @param nonNumericDefaultUncertainty the (percent) uncertainty to assign
     * measurements with a non-numeric uncertainty (i.e. LT, GT, AP, CA).
     * Should be a number between 0 and 1.
     * @param uncert Vector which will be filled with the uncertainties on
     * the fitted slopes and intercepts
     * @return vector containing the fitted slopes and intercepts, organized
     * with all of the slopes first followed by all of the intercepts
     */
    public static Matrix calculateLinearEnergyShifts(ENSDFDataset data, String standard,
            double nonNumericDefaultUncertainty, Matrix uncert){
        List<String> sources;
        List<gammaEquivalenceClass> gammaGroups, tmp;
        gammaEquivalenceClass GEC;
        int j, i, count;
        String s;
        Matrix A; //the coefficent matrix for the linear system
        Matrix W; //the weight matrix for the gamma-rays
        Matrix dE; //the vector of differences with the standard
        int m, n; //the size of the linear system
        double EStandard, E, w, wStandard;
        gamma gStandard;
        Matrix WA, WdE;
        Matrix V, uncertSq;
        
        sources = new ArrayList<>(java.util.Arrays.asList(data.getSources()));
        if(!sources.contains(standard)){
            return null;
        }
        sources.remove(standard);
        n = 2*sources.size();
        
        //group all the gammas into equivilance classes
        //ensure that the equivilance classes are non-empty and contain
        //a gamma from the standard we are comparing with
        gammaGroups = new ArrayList<>();
        m = 0;
        for(level l : data.getLevelsWithGammas()){
            tmp = ENSDFDataset.groupByGamma(l);
            for(gammaEquivalenceClass G : tmp){
                if(!G.isEmpty() && G.containsSource(standard)){
                    gammaGroups.add(G);
                    m += G.getGammas().size() - 1;
                    if(G.containsAdopted()){
                        m -= 1;
                    }
                }
            }
        }
        
        if(gammaGroups.isEmpty()){
            return null;
        }
        
        A = new Matrix(m, n);
        dE = new Matrix(m, 1);
        W = new Matrix(m, 1);
        count = 0;
        for(j=0; j<sources.size(); j++){
            s = sources.get(j);
            for(i=0; i<gammaGroups.size(); i++){
                GEC = gammaGroups.get(i);
                for(gamma g : GEC.getGammas()){
                    if(g.getSource().equals(s)){
                        gStandard = GEC.getGammaBySource(standard);
                        if (gStandard.eg.isPresent() && g.eg.isPresent()) {
                            EStandard = gStandard.eg.get();
                            E = g.eg.get();
                            dE.set(count, 0, E - EStandard);
                            A.set(count, j, -EStandard);
                            A.set(count, j+sources.size(), -1.0d);
                            try{
                                w = dataPt.constructFromString(g.getEg() + " " + g.getDEg()).getLower();
                            }catch(NullPointerException e){
                                w = nonNumericDefaultUncertainty;
                            }
                            try{
                                wStandard = dataPt.constructFromString(gStandard.getEg() + " " + gStandard.getDEg()).getLower();
                            }catch(NullPointerException e){
                                wStandard = nonNumericDefaultUncertainty;
                            }
                            W.set(count, 0, 1.0d/(w*w + wStandard*wStandard));
                        }
                        count += 1;
                    }
                }
            }
        }
        
        WA = new Matrix(m, n);
        for(j=0; j<n; j++){
            for(i=0; i<m; i++){
                WA.set(i, j, W.get(i, 0)*A.get(i, j));
            }
        }
        WdE = new Matrix(m, 1);
        for(i=0; i<m; i++){
            WdE.set(i, 0, W.get(i, 0)*dE.get(i, 0));
        }
        
        V = pseudoinverse(A.transpose().times(WA));
        uncertSq = V.times(A.transpose());
        uncertSq = uncertSq.arrayTimes(uncertSq).times(W);
        for(i=0; i<n; i++){
            uncert.set(i, 0, Math.sqrt(uncertSq.get(i, 0)));
        }
        
        return V.times(A.transpose().times(WdE));
    }
    
    /**
     * Applies the given gamma-ray energy shifts to the given dataset.
     * @param data the dataset to apply the shifts to
     * @param standard the standard dataset the shifts were calculated with
     * respect to
     * @param slopesAndIntercepts the calculated shift coefficients, see 
     * {@link #calculateLinearEnergyShifts(ENSDF_data.ENSDFDataset, java.lang.String, double, Jama.Matrix)}.
     */
    public static final void applyLinearEnergyShifts(ENSDFDataset data, String standard,
            Matrix slopesAndIntercepts){
        List<String> sources;
        List<gammaEquivalenceClass> gammaGroups, tmp;
        gammaEquivalenceClass GEC;
        int i, j;
        String s;
        gamma gStandard;
        double EStandard, E, w;
        String newEg;
        
        sources = new ArrayList<>(java.util.Arrays.asList(data.getSources()));
        if(!sources.contains(standard)){
            return;
        }
        sources.remove(standard);
        
        //set up in the same way as calculating the linear shifts
        gammaGroups = new ArrayList<>();
        for(level l : data.getLevelsWithGammas()){
            tmp = ENSDFDataset.groupByGamma(l);
            for(gammaEquivalenceClass G : tmp){
                if(!G.isEmpty()){
                    //note that were are including all the non-empty equivalence
                    //classes here (even the ones which do not have a measurement
                    //from the standard dataset, unlike when the shifts were
                    //computed. This is to allow the constant term in the shift
                    //to affect all the gamma-rays in the dataset.
                    gammaGroups.add(G);
                }
            }
        }
        
        if(gammaGroups.isEmpty()){
            return;
        }
        
        //parse in same order as for calculating the shifts
        for(j=0; j<sources.size(); j++){
            s = sources.get(j);
            for(i=0; i<gammaGroups.size(); i++){
                GEC = gammaGroups.get(i);
                for(gamma g : GEC.getGammas()){
                    if(g.getSource().equals(s)){
                        gStandard = GEC.getGammaBySource(standard);
                        if (g.eg.isPresent()) {
                            //no energy measurement from the standard dataset
                            //for this gamma-ray, so do not include the
                            //linear term, but we can still apply the 
                            //constant term
                            EStandard = gStandard.eg.orElse(0d);
                            E = g.eg.get() + 
                                    slopesAndIntercepts.get(j, 0)*EStandard + 
                                    slopesAndIntercepts.get(j+sources.size(), 0);
                            try{
                                w = dataPt.constructFromString(g.getEg() + " " + g.getDEg()).getLower();
                                g.setNumericEnergy(E, w);
                            }catch(NullPointerException e){
                                newEg = String.format("%1.0f", E);
                                g.getGammaRecord().setE(newEg);
                            }
                        }
                    }
                }
            }
        }
    }
}
