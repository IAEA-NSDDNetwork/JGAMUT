package jgamut;

import ENSDF_data.level;
import java.util.List;
import java.util.ArrayList;
import averagingAlgorithms.MathBasicFunction;
import java.util.stream.Collectors;

/**
 * This class implements the k-medoids grouping algorithm.
 * 
 * Date Modified: 18/11/2015
 * 
 * @author Michael Birch
 */
public class kMedoids {
    
    //Converts the (row, col) pair (i, j) into a linear index
    //corresponding to the index of a row-packed array containing
    //the upper triagle of elements in a symmetric matrix. Note that
    //i = j is not valid because this function is assuming the digagonal
    //is ignored (which is the case in the context of distances since
    //the distance from an element to itself is 0).
    private static int pairToInd(int i, int j, int n) {
        if (i > j) {
            return pairToInd(j, i, n);
        } else {
            return (i * (2 * n - 1 - i) / 2) + (j - i - 1);
        }
    }
    
    private static double rho(int i, int j, int n, double[] d) {
        if (i == j) { return 0d; }
        else { return d[pairToInd(i, j, n)]; }
    }
    
    /**
     * Distance between two sets of objects defined by the sum of distances
     * between all pairs (x, y), where x in X and y in Y.
     * @param X first set of objects
     * @param Y second set of objects
     * @return distance between X and Y
     */
    private static double D(List<Integer> X, List<Integer> Y, int n, double[] d){
        double tot = 0.0d;
        for(int x : X){
            for(int y : Y){
                tot += rho(x, y, n, d);
            }
        }
        return 0.5d*tot;
    }
    
    /**
     * Calculates the sum of the distances between x and each element of Y.
     * @param x
     * @param Y set of objects
     * @return the sum of the distances between x and each element of Y
     */
    private static double D(int x, List<Integer> Y, int n, double[] d){
        double tot = 0.0d;
        for(int y : Y){
            tot += rho(x, y, n, d);
        }
        return tot;
    }
    
    /**
     * Returns the minimum distance between sets of objects X and Y according to
     * min_{x in X, y in Y} rho(x, y).
     * @param X
     * @param Y
     * @return 
     */
    private static double D_min(int x, List<Integer> Y, int n, double[] d){
        double result, dist;
        
        if(Y.isEmpty()){
            return 0.0d;
        }
        
        result = rho(x, Y.get(0), n, d);
        for(int y : Y){
            dist = rho(x, y, n, d);
            result = Math.min(result, dist);
        }
        return result;
    }
    
    /**
     * Groups the element in Y given the medoids M. Each medoid is in its own
     * group and each y in Y is assigned to belong to the group of the medoid
     * it is closest to.
     * @param M the medoids
     * @param Y the set of elements to group
     * @return the groups containing the elements of Y
     */
    private static List<List<Integer>> makeGroups(List<Integer> M, List<Integer> Y, int n, double[] ds){
        List<List<Integer>> G = new ArrayList<>();
        int k = M.size();
        double[] d;
        int i;
        
        //put the medoids each into their own group
        for(i=0; i<k; i++){
            G.add(new ArrayList<>());
            G.get(i).add(M.get(i));
        }
        
        d = new double[k];
        for(int y : Y){
            for(i=0; i<k; i++){
                d[i] = rho(y, M.get(i), n, ds);
            }
            i = MathBasicFunction.minInd(d);
            if(!M.contains(y)){ //if y not in M (otherwise would already be in the group)
                G.get(i).add(y); //place y in the group of the closest medoid
            }
        }
        return G;
    }
    
    /**
     * Groups the elements of Y into k groups using the Partitioning Around Medoids (PAM) algorithm
     * @param k number of groups
     * @param Y elements to group
     * @param M list which will store the medoids of the groups (should be empty, but not null)
     * @return the optimal groupings according to PAM
     */
    private static List<List<Integer>> PAM(int k, List<Integer> Y, List<Integer> M, int n, double[] ds){
        int i, m;
        boolean flag;
        double Tih, F;
        
        //choose initial condition with the medoids evenly spread
        //throughout the data
        int di = Y.size()/k;
        for(i=0; i<Y.size() && M.size() < k; i+=di){
            M.add(Y.get(i));
        }
        if(M.size() != k){ M.add(Y.get(Y.size() - 1)); }
        
        flag = false;
        while(!flag){
            flag = true;
            for(int h : Y){
                if(M.contains(h)){
                    continue;
                }
                for(i=0; i<k; i++){
                    m = M.get(i);
                    M.remove(i);
                    Tih = Math.min(D_min(m, M, n, ds), rho(m, h, n, ds)); //cost of from m of removing m from M and adding h to M
                    Tih -= Math.min(D_min(h, M, n, ds), rho(h, m, n, ds)); //cost from h of adding h to M
                    for(int j : Y){
                        if(j == h || j == m || M.contains(j)){
                            continue;
                        }
                        F = D_min(j, M, n, ds);
                        Tih += Math.max(0.0d, F - rho(j, m, n, ds)); //cost from j of removing m from M
                        Tih += Math.min(0.0d, rho(j, h, n, ds) - F); //cost from j of adding h to M
                    }
                    if(Tih < 0.0d){
                        M.add(h);
                        flag = false;
                    }else{
                        M.add(m);
                    }
                }
            }
        }
        
        return makeGroups(M, Y, n, ds);
    }
    private static List<List<Integer>> PAM(int k, List<Integer> Y, int n, double[] ds) {
        return PAM(k, Y, new ArrayList<>(), n, ds);
    }
    
    //silhouette
    private static double s(int y, List<List<Integer>> G, int n, double[] ds){
        int k = G.size();
        int i, groupInd;
        double[] b;
        double a;
        
        b = new double[k];
        groupInd = -1;
        for(i=0; i<k; i++){
            if(G.get(i).contains(y)){
                groupInd = i;
            }
            //b[i] is the average distance of y from the other clusters
            b[i] = D(y, G.get(i), n, ds)/((double)G.get(i).size());
        }
        a = b[groupInd]; //a is the average distance of y to data points in it's own cluster
        b[groupInd] = MathBasicFunction.sum(b); //make this element of b larger than all other elements so it will not be the minimum
        i = MathBasicFunction.minInd(b);
        
        return (b[i] - a)/Math.max(b[i], a);
    }
    
    private static double silhouette(List<Integer> Y, List<List<Integer>> G, int n, double[] ds){
        double sTot = 0.0d;
        for(int y : Y){
            sTot += s(y, G, n, ds);
        }
        return sTot/((double)Y.size());
    }
    
    private static double variationRatio(List<List<Integer>> G, int n, double[] ds){
        double out = 0.0d; //external variation
        double in = 0.0d; //interanl variation
        for(List<Integer> g : G){
            in += D(g, g, n, ds);
            for(List<Integer> g2 : G){
                if(g2 != g){
                    out += D(g, g2, n, ds);
                }
            }
        }
        return in/out;
    }
    
    public static List<List<level>> doGrouping(List<level> levels, int kmin){
        double[] distances; //upper triagular matrix of distances
        int n = levels.size();
        double[] sil, var, dvar;
        List<Integer> M, Y;
        List<List<Integer>> G;
        int k;
        boolean useVar = false;
        
        //exactly the same number of elements to group as the
        //minimum number of groups, so there must be one element per
        //group
        if(n == kmin){
            List<List<level>> result = new ArrayList<>();
            for(level y : levels){
                result.add(new ArrayList<>());
                result.get(result.size() - 1).add(y);
            }
            return result;
        }
        
        //build up distances matrix
        distances = new double[n * (n - 1) / 2];
        for(int i=0; i < n - 1; i++) {
            level elem = levels.get(i);
            for(int j = i + 1; j < n; j++) {
                int idx = pairToInd(i, j, n);
                level elemj = levels.get(j);
                distances[idx] = level.distance(elem, elemj);
            }
        }
        
        Y = new ArrayList<>();
        for(int i=0; i < n; i++) { Y.add(i); }
        
        sil = new double[n-kmin+1];
        var = new double[n-kmin+1];
        dvar = new double[n-kmin-1+1];
        M = new ArrayList<>();
        for(k=kmin; k <= n; k++){
            M.clear();
            G = PAM(k, Y, M, n, distances);
            sil[k-kmin] = silhouette(Y, G, n, distances);
            var[k-kmin] = Math.log(variationRatio(G, n, distances));
            if (sil[k-kmin] > 0.9999) {
                //stop searching if the value is increadibly close to 1
                break;
            }
            if(k > kmin){
                dvar[k-kmin-1] = var[k-kmin] - var[k-kmin-1];
                if((sil[k-kmin-1] > 0.95) && (sil[k-kmin] < sil[k-kmin-1])){
                    //stop searching after first maximum better than 0.95
                    break;
                }else if(dvar[k-kmin-1] < -3.0d ){
                    useVar = true;
                    break;
                }
            }
        }
        if(useVar){
            k = MathBasicFunction.minInd(dvar)+kmin+1;
        }else{
            k = MathBasicFunction.maxInd(sil)+kmin;
        }
        return PAM(k, Y, n, distances)
                .stream()
                .map(g -> g.stream().map(i -> levels.get(i)).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
