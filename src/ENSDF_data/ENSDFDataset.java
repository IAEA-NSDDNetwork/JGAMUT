
package ENSDF_data;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import averagingAlgorithms.*;
import ensdf_datapoint.dataPt;
import javax.swing.JOptionPane;
import text_io.textTable;
import Jama.Matrix;
import ensdf_datapoint.fmtHandler;
import java.util.Arrays;
import jgamut.JGAMUT_GUI;
import jgamut.kMedoids;

/**
 * This class defines the data structure for ENSDF datasets as well as methods
 * for manipulating ENSDFDataset objects.
 * 
 * Date Modified: 21/08/2015
 * 
 * @author Michael Birch
 */
public class ENSDFDataset {
    /**
     * The nucleus ID (NUCID) used for the dataset (see ENSDF manual)
     */
    private String nucid;
    /**
     * The title of the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     */
    private final String title;
    /**
     * The references for the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     */
    private final String refs;
    /**
     * List of levels observed in the dataset
     */
    private List<level> Levels;
    /**
     * List of gammas observed in the dataset
     */
    private List<gamma> Gammas;
    /**
     * The lines from the original ENS text file which was used
     * to generate the ENSDFDataset object
     */
    private List<String> Lines;
    
    /**
     * Creates a new ENSDFDataset object with no levels, gammas, or text lines.
     * @param N the nucleus ID (NUCID) used for the dataset (see ENSDF manual)
     * @param T the title of the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     * @param R the references for the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     */
    public ENSDFDataset(String N, String T, String R){
        this.nucid = N;
        this.title = T;
        this.refs = R;
        this.Levels = new ArrayList<>();
        this.Gammas = new ArrayList<>();
        this.Lines = null;
    }
    
    /**
     * Create an ENSDFDataset object from the output of the 
     * {@link jgamut.GAMUT_Algorithms GAMUT Algorithms}.
     * @param data the dataset on which the GAMUT algorithms ran
     * @param L the output level energies from the GAMUT algorithms
     * @param dL the uncertainties in the output level energies
     * @param Eg the output gamma energies from the GAMUT algorithms
     * @param dEg the uncertainties in the output gamma energies
     * @param Ig the output gamma intensities from the GAMUT algorithms
     * @param dIg the uncertainties in the output gamma intensities
     * @param decayData <code>true</code> if the data should be normalized like
     * a decay dataset (as opposed to an Adopted Levels, Gammas dataset
     */
    public ENSDFDataset(ENSDFDataset data, Matrix L, Matrix dL, Matrix Eg, 
            Matrix dEg, Matrix Ig, Matrix dIg, boolean decayData){
        this.nucid = data.nucid;
        this.refs = "";
        this.title = "GMAUT";
        this.Levels = new ArrayList<>();
        this.Gammas = new ArrayList<>();
        this.Lines = null;
        
        List<String> parentFinalPairs = new ArrayList<>();
        String parentFinalPairString;
        
        gamma[] oldGammas = data.getGammas();
        level[] oldLevels = data.getLevelsWithGammas();
        String[] oldSources = data.getSources();
        
        List<gammaEquivalenceClass> tmp;
        boolean hasNonAdopted;
        int i, j, k, count, parentIndex, finalIndex;
        String s;
        gamma g,newg;
        level p, f;
        
        for(k=0; k<oldLevels.length; k++){ //assign level energies
            this.Levels.add(new level(new ENSDFLevelRecord(oldLevels[k].getLvlRecord().toString()), "GAMUT RESULT"));
            if(L.get(k, 0) > 1e-5){ //don't change the zero level. It's still zero
                this.getLastLevel().setNumericEnergy(L.get(k, 0), dL.get(k, 0));
            }
        }
        count = 0;
        parentIndex = 0;
        finalIndex = 0;
        //parse in same order as to make Eg column vector (see GAMUT_Algorithms)
        for(i=0; i<oldSources.length; i++){
            s = oldSources[i];
            for(j=0; j<oldGammas.length; j++){
                g = oldGammas[j];
                if(g.getCMEnergy().toDouble() <= 0.0d){
                    continue; //skip gammas with no energy since they did not
                              //contribuate to the energy fitting
                }
                if(g.getSource().equals(s)){
                    p = g.getParent();
                    f = g.getFinalLevel();
                    if(p == null || f == null){ 
                        continue;
                    }
                    for(k=0; k<oldLevels.length; k++){ //loop over levels to find parent/final indecies
                        if(p.equals(oldLevels[k])){
                            parentIndex = k;
                        }else if(f.equals(oldLevels[k])){
                            finalIndex = k;
                        }
                    }
                    parentFinalPairString = String.valueOf(parentIndex) + " " +
                            String.valueOf(finalIndex);
                    //only add each gamma ray (i.e. transition between this pair of levels) once
                    if(!parentFinalPairs.contains(parentFinalPairString)){
                        parentFinalPairs.add(parentFinalPairString);
                        newg = new gamma(new ENSDFGammaRecord(g.getGammaRecord().toString()), "GAMUT RESULT");
                        newg.setNumericEnergy(Eg.get(count, 0) - newg.recoilCorrection(), dEg.get(count, 0));
                        newg.setIg(""); //intensity is blank by default
                        newg.setDIg("");
                        this.addGamma(newg, this.Levels.get(parentIndex));
                        this.getLastGamma().setFinalLevel(Levels.get(finalIndex));
                    }
                    count += 1;
                }
            }
        }
        
        //parse intensities in same order as Ig (see GAMUT_Algorithms)
        i=0;
        for(k=0; k<oldLevels.length; k++){
            tmp = ENSDFDataset.groupByGamma(oldLevels[k]);
            for(gammaEquivalenceClass G : tmp){
                if(G.isEmpty()){
                    continue;
                }
                hasNonAdopted = false;
                for(gamma gam : G.getGammas()){
                    if(!gam.isAdopted()){
                        hasNonAdopted = true;
                        break;
                    }
                }
                if(!hasNonAdopted){
                    continue;
                }
                if(Ig.get(i, 0) > 0.0d){
                    g = G.getGammas().get(0);
                    f = g.getFinalLevel();
                    finalIndex = -1;
                    for(j=0; j<oldLevels.length; j++){
                        if(f.equals(oldLevels[j])){
                            finalIndex = j;
                            break;
                        }
                    }
                    if(finalIndex == -1){ //final level not found
                        continue;
                    }
                    for(gamma gam : this.Gammas){
                        if(gam.getParent().equals(this.Levels.get(k)) &&
                                gam.getFinalLevel().equals(this.Levels.get(finalIndex))){
                            gam.setNumericIntensity(Ig.get(i, 0), dIg.get(i, 0));
                            break;
                        }
                    }
                }
                i += 1;
            }
        }
        //sort gammas by energy
        for(level l : this.Levels){
            Collections.sort(l.getGammaOutList(), gamma.energyComparator);
        }
        this.renormalizeGammaRays(decayData);
    }
    
    /**
     * Set the {@link #Lines} member to be <code>l</code>.
     * @param l new value of the {@link #Lines} member
     */
    public final void setLines(List<String> l){
        this.Lines = l;
    }
    
    /**
     * Add a new level to the dataset.
     * @param l the level to add
     * @param copyLevel if <code>true</code> then the level object will
     * be copied to a new level object before being added to the dataset
     * (to prevent accidentally modifying a different level while modifying
     * the level in this dataset or vice versa)
     */
    public final void addLevel(level l, boolean copyLevel){
        if(copyLevel){
            this.Levels.add(new level(l));
        }else{
            this.Levels.add(l);
        }
        this.nucid = l.getLvlRecord().getNucid();
        this.Levels.get(this.Levels.size() - 1).removeAllGammas();
    }
    public final void addLevel(level l){
        addLevel(l, true);
    }
    /**
     * Add a new gamma to the dataset.
     * @param g the new gamma to add
     * @param parent the level which this gamma depopulates
     */
    public final void addGamma(gamma g, level parent){
        int i;
        this.Gammas.add(new gamma(g));
        i = this.Gammas.size() - 1;
        if(parent != null){
            this.Gammas.get(i).setParent(parent);
        }
    }
    
    /**
     * Returns a sorted array of all the unique dataset titles the gamma rays
     * in this dataset have. The ADOPTED LEVELS, GAMMAS dataset is not included
     * in this array.
     * @return a sorted array of all the unique dataset titles the gamma rays
     * in this dataset have.
     */
    public final String[] getSources(){
        Set<String> sources = new TreeSet<>();
        for(gamma g : this.Gammas){
            if(!g.isAdopted()){
                sources.add(g.getSource());
            }
        }
        return sources.toArray(new String[0]);
    }
    /**
     * Returns the number of unique dataset titles the gamma rays
     * in this dataset have.
     * @return the number of unique dataset titles the gamma rays
     * in this dataset have.
     */
    public final int getNumSources(){
        Set<String> sources = new TreeSet<>();
        for(gamma g : this.Gammas){
            if(!g.isAdopted()){
                sources.add(g.getSource());
            }
        }
        return sources.size();
    }
    
    /**
     * Returns the gamma-rays in this dataset which have energy greater
     * than zero. I.e. only returns gammas which actually have an energy
     * measurement.
     * @return Returns the gamma-rays in this dataset which have energy greater
     * than zero
     */
    public final gamma[] getNonzeroGammas(){
        List<gamma> result;
        
        result = new ArrayList<>();
        for(gamma g : this.Gammas){
            if(g.getEnergy().toDouble() > 0.0d){
                result.add(g);
            }
        }
        return result.toArray(new gamma[0]);
    }
    /**
     * Returns the number of gamma-rays with energy greater than zero. I.e.
     * the number of gamma-rays which actually have an energy measurement.
     * @return the number of gamma-rays with energy greater than zero
     */
    public final int getNumNonzeroGammas(){
        int result;
        
        result = 0;
        for(gamma g : this.Gammas){
            if(g.getEnergy().toDouble() > 0.0d){
                result += 1;
            }
        }
        return result;
    }
    /**
     * Returns the gamma-rays in this dataset
     * @return the gamma-rays in the dataset
     */
    public final gamma[] getGammas(){
        return this.Gammas.toArray(new gamma[0]);
    }
    /**
     * Returns the number of gamma-rays in this dataset
     * @return the number of gamma-rays in this dataset
     */
    public final int getNumGammas(){
        return this.Gammas.size();
    }
    /**
     * Returns the number of unique gamma-rays in this dataset. I.e. does not
     * double count gamma-rays which are the same (in the sense of having the same
     * initial and final levels), but appear more than once in the dataset.
     * Such a situation can arise when using the constructor that uses the
     * intermediate file since the resulting dataset will include all the
     * measurements of each gamma-ray.
     * @return the number of unique gamma-rays in this dataset
     */
    public final int getNumUniqueGammas(){
        List<gammaEquivalenceClass> gammaGroups, tmp;
        gammaGroups = new ArrayList<>();
        for(level l : this.Levels){
            tmp = groupByGamma(l);
            for(gammaEquivalenceClass G : tmp){
                if(!G.isEmpty()){
                    for(gamma g : G.getGammas()){
                        if(!g.isAdopted()){
                            gammaGroups.add(G);
                            break;
                        }
                    }
                }
            }
        }
        return gammaGroups.size();
    }
    /**
     * Returns the number of gamma-rays in this dataset which come from the
     * ADOPTED LEVELS, GAMMAS dataset.
     * @return the number of gamma-rays in this dataset which come from the
     * ADOPTED LEVELS, GAMMAS dataset.
     */
    public final int getNumAdoptedGammas(){
        int result = 0;
        for(gamma g : Gammas){
            if(g.isAdopted()){
                result += 1;
            }
        }
        return result;
    }
    /**
     * Returns the last gamma-ray added to the <code>Gammas</code> List, and
     * <code>null</code> if the list is empty.
     * @return the last gamma-ray added to the <code>Gammas</code> List, and
     * <code>null</code> if the list is empty.
     */
    public final gamma getLastGamma(){
        if (this.Gammas.isEmpty()){
            return null;
        }
        return this.Gammas.get(this.Gammas.size() - 1);
    }
    
    /**
     * Returns the gamma-ray in the dataset (if any) with parent level p
     * and final level f.
     * @param p the parent level of the gamma
     * @param f the final level of the gamma
     * @return the gamma-ray in the dataset (if any) with parent level p
     * and final level f.
     */
    public final gamma getGamma(level p, level f){
        for(gamma g : Gammas){
            if(g.getParent().equals(p)){
                if(f != null){
                    if(g.getFinalLevel() != null){
                        if(g.getFinalLevel().equals(f)){
                            return g;
                        }
                    }
                }else{
                    if(g.getFinalLevel() == null){
                        return g;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the levels contained in this dataset.
     * @return the levels contained in this dataset
     */
    public final level[] getLevels(){
        return this.Levels.toArray(new level[0]);
    }
    /**
     * Returns the number of levels in this dataset.
     * @return the number of levels in this dataset
     */
    public final int getNumLevels(){
        return this.Levels.size();
    }
    /**
     * Returns only the levels in this dataset which have gamma-rays
     * (going into or coming out of the level) associated with it which are not
     * from the ADOPTED LEVELS, GAMMAS dataset. Adopted gammas are ignored
     * because they should not contribute to any fitting/averaging that is
     * done.
     * @return only the levels in this dataset which have gamma-rays
     * (going into or coming out of the level) associated with it which are not
     * from the ADOPTED LEVELS, GAMMAS dataset
     */
    public final level[] getLevelsWithGammas(){
        List<level> result = new ArrayList<>();
        for(level l : Levels){
            if(l.hasNonAdoptedGammas()){
                result.add(l);
            }
        }
        return result.toArray(new level[0]);
    }
    /**
     * Returns the number of levels in this dataset which have gamma-rays
     * associated with it which are not from the ADOPTED LEVELS, GAMMAS dataset.
     * @return the number of levels in this dataset which have gamma-rays
     * associated with it which are not from the ADOPTED LEVELS, GAMMAS dataset.
     */
    public final int getNumLevelsWithGammas(){
        int result = 0;
        for(level l : Levels){
            if(l.hasNonAdoptedGammas())
                result += 1;
        }
        return result;
    }
    /**
     * Returns the number of levels in this dataset which have zero energy. 
     * This number could be greater than 1 if there are floating level structures
     * not connected to the main level scheme, e.g <code>0+X</code> and <code>
     * X</code> count as additional zero levels.
     * @return the number of levels in this dataset which have zero energy. 
     */
    public final int getNumZeroLevels(){
        int result = 0;
        for(level l: Levels){
            if(l.getEnergy().toDouble() < 1e-10){
                result += 1;
            }
        }
        return result;
    }
    /**
     * Returns the last level added to the <code>Levels</code> List.
     * @return the last level added to the <code>Levels</code> List
     */
    public final level getLastLevel(){
        if (this.Levels.isEmpty()){
            return null;
        }
        return this.Levels.get(this.Levels.size() - 1);
    }
    /**
     * Returns the level with the specified index.
     * @param ind index of the desired level
     * @return the level with the specified index.
     */
    public level getLevel(int ind){
        return this.Levels.get(ind);
    }
    /**
     * Returns the level with energy closest to the specified energy. The
     * non-numeric part of the energy (if any) must also match. E.g. 
     * <code>100+X</code> is not the same as <code>100+Y</code>.
     * @param E energy of the desired level
     * @return the level with energy closest to the specified energy
     */
    public level getLevel(ENSDFEnergy E){
        List<Double> Ediffs;
        List<level> lvls;
        Double minDiff;
        level result;
        
        Ediffs = new ArrayList<>();
        lvls = new ArrayList<>();
        for(level l : Levels){
            if(l.getEnergy().isNonnumericMatch(E)){
                lvls.add(l);
                Ediffs.add(Math.abs(l.getEnergy().doubleDiff(E)));
            }
        }
        if(lvls.isEmpty()){
            return null;
        }else{
            minDiff = Collections.min(Ediffs);
            result = lvls.get(Ediffs.indexOf(minDiff));
            return result;
        }
    }
    
    /**
     * Returns the level with energy which EXACTLY matches the given String.
     * @param energy the energy String to match
     * @return the level with energy which EXACTLY matches the given String.
     */
    public level getLevel(String energy){
        for(level l : Levels){
            if(l.getLvlRecord().getE().equals(energy)){
                return l;
            }
        }
        return null;
    }
    /**
     * Returns the nucleus ID (NUCID) used for the dataset (see ENSDF manual)
     * @return the nucleus ID (NUCID) used for the dataset (see ENSDF manual)
     */
    public String getNucid(){
        return this.nucid;
    }
    /**
     * Returns the title of the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     * @return the title of the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     */
    public String getTitle(){
        return this.title;
    }
    /**
     * Returns the references for the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     * @return the references for the ENSDF dataset given in the Identification record
     * (see ENSDF manual)
     */
    public String getRefs(){
        return this.refs;
    }
    
    /**
     * This function assigns final levels to all the gamma rays in the dataset.
     */
    public final void findFinalLevels(){
        level fl;
        for(gamma g : Gammas){
            if(g.hasFL()){ //use 'FL=' field if it is present
                fl = getLevel(g.getFL());
                if(fl == null){
                    JOptionPane.showMessageDialog(null, "Warning! No level matches FL=" + g.getFL() + ".", "FL Warning", JOptionPane.WARNING_MESSAGE);
                }
            }else{
                fl = getLevel(g.getParent().getEnergy().diff(g.getEnergy()));
                if(fl == null){
                    JOptionPane.showMessageDialog(null, "Warning! No final level found for gamma ray " + 
                            g.getEg() + " from level " + g.getParent().getLvlRecord().getE(), "No Final Level Warning", JOptionPane.WARNING_MESSAGE);
                }else if(Math.abs(g.getParent().getEnergy().diff(g.getEnergy()).doubleDiff(fl.getEnergy())) > 50d){
                    JOptionPane.showMessageDialog(null, "Warning! Poor level final energy match for gamma ray " + 
                            g.getEg() + " from level " + g.getParent().getLvlRecord().getE() + " in dataset " +
                            this.title, "Poor Final Level Match Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
            g.setFinalLevel(fl);
        }
    }
    
    
    /**
     * Normalizes gamma rays so that the strongest
     * gamma ray in the dataset has intensity equal to 100. This is
     * the convention for decay datasets.
     */
    public final void renormalizeGammaRays_decay(){
        double maxIg;
        double newIg, newDIg;
        dataPt intensityDataPt;
        String tmp;
        
        maxIg = -1.0d;
        for(gamma g : this.Gammas){
            if (g.ig.isPresent()) {
                maxIg = Math.max(maxIg, g.ig.get());
            }
        }
        if(maxIg < 0.0d){
            //this can only occur if there are no gammas with measured
            //intensities, so there's nothing to do
            return;
        }
        for(gamma g : this.Gammas){
            intensityDataPt = g.getIntensityDataPt();
            if(intensityDataPt == null){
                if (g.ig.isPresent()) {
                    newIg = g.ig.get()*100.0d/maxIg;
                    g.setIg_renorm(String.valueOf(Math.round(newIg)));
                } else {
                    g.setIg_renorm("");
                }
                g.setDIg_renorm(g.getDIg());
            }else{
                newIg = intensityDataPt.getValue()*100.0d/maxIg;
                newDIg = 100d * intensityDataPt.getLower() / maxIg;
                
                tmp = dataPt.ENSDFprint(String.valueOf(newIg), 
                        String.valueOf(newDIg), false);
                g.setIg_renorm(tmp.split(" ")[0]);
                g.setDIg_renorm(tmp.split(" ")[1]);
            }
        }
    }
    
    /**
     * Normalizes gamma rays so that for each level the strongest
     * gamma ray has intensity equal to 100. This is the convention for
     * the ADOPTED LEVELS, GAMMAS dataset.
     */
    public final void renormalizeGammaRays_adopted(){
        int i,j;
        gamma[] gammas;
        double[] intensities;
        double maxI;
        dataPt intensityDataPt;
        String[] tmp;
        
        for(i=0; i<this.Levels.size(); i++){
            gammas = this.Levels.get(i).getOutGRays();
            if(gammas.length == 0){
                continue; //skip levels with no gamma rays
            }else if(gammas.length == 1){
                //for levels with just one gamma ray the intensity is
                //100 by definition so there is no uncertainty
                gammas[0].setIg_renorm("100");
                gammas[0].setDIg_renorm("");
                continue;
            }
            intensities = new double[gammas.length];
            for(j=0; j<gammas.length; j++){
                intensities[j] = gammas[j].ig.orElse(0d);
            }
            maxI = MathBasicFunction.max(intensities);
            if(maxI < 1e-20){ //max intensity was probably zero, don't divide by zero
                continue;
            }
            for(j=0; j<gammas.length; j++){
                intensityDataPt = gammas[j].getIntensityDataPt();
                if(intensityDataPt == null){
                    if(intensities[j] == 0d){
                        gammas[j].setIg_renorm("");
                    }else{
                        gammas[j].setIg_renorm(
                                String.valueOf(
                                    // Round to nearest whole when non-numeric uncertainty
                                    Math.round(100d * intensities[j] / maxI)
                                )
                        );
                    }
                    gammas[j].setDIg_renorm(gammas[j].getDIg());
                }else{
                    intensityDataPt.setValue(100d * intensities[j] / maxI);
                    intensityDataPt.setLower(100d * intensityDataPt.getLower() / maxI);
                    intensityDataPt.setUpper(intensityDataPt.getLower());
                    
                    tmp = intensityDataPt.toString(false, true).split("\\(");
                    gammas[j].setIg_renorm(tmp[0]);
                    gammas[j].setDIg_renorm(tmp[1].replace(")", ""));
                }
            }
        }
    }
    
    public final void renormalizeGammaRays(boolean decayData){
        if(decayData){
            renormalizeGammaRays_decay();
        }else{
            renormalizeGammaRays_adopted();
        }
    }
    
    /**
     * Determines if there are still unmatched levels in the list of datasets.
     * @param datasets
     * @return <code>true</code> if there are still unmatched levels in 
     * the list of datasets.
     */
    public static final boolean hasUnmatchedLevels(List<ENSDFDataset> datasets){
        for(ENSDFDataset d : datasets){
            for(level l : d.Levels){
                if(!l.matched){
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Determines if there are still unmatched levels in the dataset.
     * @param d the ENSDF dataset
     * @return <code>true</code> if there are still unmatched levels in 
     * the dataset.
     */
    public static final boolean hasUnmatchedLevels(ENSDFDataset d){
        for(level l : d.Levels){
            if(!l.matched){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if there are still unmatched gammas in the list.
     * @param gammas the list of gammas to check
     * @return <code>true</code> if there are still unmatched gammas in 
     * the list.
     */
    public static final boolean hasUnmatchedGammas(List<gamma> gammas){
        for(gamma g : gammas){
            if(!g.matched){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Combination constructor. Creates a single dataset from many inputs by
     * identifying which levels are the same between datasets and mapping
     * those to a single level in the output dataset. 
     * @param datasets input datasets (from different experiments)
     * @param dE_max the size of the energy jump which indicates a new
     * set of levels has been reached (i.e. the difference in energy which
     * guarantees that two levels are different and not the same level measured
     * in different experiments)
     */
    public ENSDFDataset(List<ENSDFDataset> datasets, double dE_max, JGAMUT_GUI.updateStatus updater){
        List<List<level>> G, allGroups;
        ArrayList<level> allLevels, Y;
        double E, dE;
        int i, j, N, adoptedCount;
        
        this.nucid = datasets.get(0).nucid;
        this.title = "Combined Dataset";
        this.refs = "";
        this.Levels = new ArrayList<>();
        this.Gammas = new ArrayList<>();
        this.Lines = null;
        
        allLevels = new ArrayList<>();
        for(ENSDFDataset d : datasets){
            if(d.getGammas().length == 0){
                continue;
            }
            allLevels.addAll(Arrays.asList(d.getLevels()));
        }
        allLevels.sort(level.energyComparator); //sort by energy
        
        Y = new ArrayList<>();
        allGroups = new ArrayList<>();
        Y.add(allLevels.get(0)); //start with the first level in the list
        N = allLevels.size();
        i=1; //index begins at 1 since 0 already added
        while(i < N){
            //add levels until two jumps of size 100 keV have been seen
            //to ensure there are at least two entire groups of levels
            j = 0;
            adoptedCount = 0;
            //We put a limit on Y because the grouping algorithm
            //scales horribly and it just becomes too costly to run.
            //This will introduce some duplicate levels, but completing
            //is better than not completing.
            while(i < N && j < 2 && Y.size() < 90){
                if (allLevels.get(i).isAdopted()) {
                    adoptedCount += 1;
                }
                dE = Math.abs(allLevels.get(i).getEnergy().
                        doubleDiff(allLevels.get(i-1).getEnergy()));
                if(dE < dE_max){
                    Y.add(allLevels.get(i));
                    i++;
                }else{
                    j++;
                    if (adoptedCount > 2) {
                        //Regardless of how many jumps have occurred,
                        //we know we have encountered more than one level
                        j = 2;
                    } else if(j < 2){ //only one jump has occurred
                        Y.add(allLevels.get(i));
                        i++;
                    }
                }
            }
            G = kMedoids.doGrouping(Y, Math.max(2, Y.size()/datasets.size())); //group levels in Y
            allGroups.addAll(G); //add those groups to allGroups
            updater.setMessage("Status: Matching Levels... (" + 
                    String.format("%1.0f", 100.0d*(double)i/(double)N) + "%)");
            Y.clear(); //clear Y
            if(i < allLevels.size()){ //add the first level from the next set
                Y.add(allLevels.get(i));
                i++;
            }
        }
        
        for(List<level> Lg : allGroups){
            this.Levels.add((new levelEquivalenceClass(Lg)).toSingleLevel());
            for(level l : Lg){
                for(gamma g : l.getGammaOutList()){
                    this.addGamma(new gamma(g.getGammaRecord(), g.getSource()),
                                  this.getLastLevel());
                    this.getLastGamma().setIg_renorm(g.getIg(true));
                    this.getLastGamma().setDIg_renorm(g.getDIg(true));
                    if(g.hasFL()){ //preserve FL= information
                        for(List<level> Lg2 : allGroups){
                            if(Lg2.contains(g.getFinalLevel())){
                                this.getLastGamma().addGammaContinuationRecord(
                                new ENSDFGammaContinuationRecord(this.nucid + "2 G FL=" + 
                                        (new levelEquivalenceClass(Lg2)).
                                                toSingleLevel().getLvlRecord().getE()));
                                break;
                            }
                        }
                    }
                }
            }
        }
        this.findFinalLevels(); //assign final levels
        Collections.sort(this.Levels, level.energyComparator); //sort levels by increasing energy
    }

    /**
     * For gamma rays which have a DE field equal to
     * any of "GT", "LT", "LE", "GE", "CA", or "SY", this method
     * sets the E and DE (energy) fields both to the empty string; the same
     * is done for the RI and DRI (intensity) fields.
     */
    public final void removeGammasWithBadUncert() {
        String dEgamma;
        String dIgamma;
        java.util.function.Function<String, Boolean> isBad;
        
        isBad = (String uncert) -> {
            return uncert.equals("GT") || 
                    uncert.equals("LT") ||
                    uncert.equals("LE") ||
                    uncert.equals("GE") ||
                    uncert.equals("CA") ||
                    uncert.equals("SY");
        };
        
        for(gamma g : this.Gammas) {
            dEgamma = g.getDEg().toUpperCase();
            dIgamma = g.getDIg().toUpperCase();
            if(isBad.apply(dEgamma)){
                g.setEg("");
                g.setDEg("");
            }
            if(isBad.apply(dIgamma) || !g.getIntensitySymbol().isEmpty()){
                g.setIg("");
                g.setDIg("");
            }
        }
    }
    
    /**
     * Groups the gamma-rays which have the specified level as their initial
     * level such that all gammas in a group are the same in the sense of
     * having the same initial and final levels.
     * @param l the initial level of the gammas to group
     * @return a List of gammaEquivilenceClasses, each class contains a group
     * of gammas which are the same
     */
    public static final List<gammaEquivalenceClass> groupByGamma(level l){
        List<gammaEquivalenceClass> result;
        int count;
        List<gamma> gammas = new ArrayList<>();
        String lvlString = trimTail(l.getLvlRecord().toString().substring(0, 39));
        boolean added;
        
        gammas.addAll(l.getGammaOutList());
        gammas.sort(gamma.energyComparator);
        result = new ArrayList<>();
        result.add(new gammaEquivalenceClass());
        count = 0;
        result.get(count).setLevelString(lvlString);
        
        for(gamma g : gammas){
            added = false;
            for(gammaEquivalenceClass G : result){
                if(G.belongs(g)){
                    G.add(g);
                    added = true;
                    break;
                }
            }
            if(!added){
                result.get(count).sort();
                result.add(new gammaEquivalenceClass());
                count += 1;
                result.get(count).setLevelString(lvlString);
                result.get(count).add(g);
            }
        }
        result.get(count).sort();
        
        return result;
    }
    /**
     * Groups the given gammas such that all gammas in a group are the same in the sense of
     * having the same initial and final levels.
     * @param gammalist the gammas to group
     * @return a List of gammaEquivilenceClasses, each class contains a group
     * of gammas which are the same
     */
    public static final List<gammaEquivalenceClass> groupByGamma(List<gamma> gammalist){
        List<gammaEquivalenceClass> result;
        int count;
        
        result = new ArrayList<>();
        result.add(new gammaEquivalenceClass());
        count = 0;
        
        for(gamma g : gammalist){
            g.matched = false;
        }
        while(hasUnmatchedGammas(gammalist)){
            for(gamma g : gammalist){
                if(!g.matched && result.get(count).belongs(g)){
                    result.get(count).add(g);
                    g.matched = true;
                }
            }
            if(!result.get(count).isEmpty()){
                result.get(count).sort();
                count += 1;
                result.add(new gammaEquivalenceClass());
            }
        }
        
        return result;
    }

    /**
     * Returns a new String with the tailing whitespace removed from 
     * the given String.
     * @param text the String from which to remove the tailing whitespace
     * @return a new String with the tailing whitespace removed from 
     * the given String.
     */
    private static String trimTail(String text){
        return text.replaceFirst("\\s+$", "");
    }
    
    /**
     * Generates a truncated table similar to the intermediate file table. This
     * table is used for the gamma context window in the GAMUT chi^2 analysis. As
     * a side effect, this function sets the first value of <code>ind</code> to
     * be the line number on which the specified gamma appears in the table
     * @param gammaGroups groups of gammas that are the same. These gammas are the
     * ones listed in the table
     * @param ind line index of the specified gamma
     * @param gam the specified gamma
     * @return a truncated table similar to the intermediate file table
     */
    public static final List<String> generateMiniDatasetTable(List<gammaEquivalenceClass> gammaGroups,
            int[] ind, gamma gam){
        textTable result = new textTable();
        int count, groupIndex;
        
        count = 0;
        for(groupIndex = 0; groupIndex < gammaGroups.size(); groupIndex++){
            if(gammaGroups.get(groupIndex).isEmpty()){
                continue;
            }
            if(groupIndex == 0 || !gammaGroups.get(groupIndex).getLevelString()
                    .equals(gammaGroups.get(groupIndex-1).getLevelString())){
                result.setCell(count, 0, gammaGroups.get(0).getLevelString());
                count += 1;
                result.setCell(count, 0, "Data set");
                result.setCell(count, 1, "Eg");
                result.setCell(count, 2, "Ig (orig.)");
                result.setCell(count, 3, "Ig (norm.)");
                count += 1;
            }
            for(gamma g : gammaGroups.get(groupIndex).getGammas()){
                if(gam.equals(g)){
                    ind[0] = count;
                }
                result.setCell(count, 0, g.getSource());
                result.setCell(count, 1, g.getEg() + " " + g.getDEg());
                result.setCell(count, 2, g.getIg() + " " + g.getDIg());
                result.setCell(count, 3, g.getIg(true) + " " + g.getDIg(true));
                count += 1;
            }
            count += 1;
        }
        return result.toStringList();
    }
    
    /**
     * Generates the intermediate file from the dataset created by combining the
     * input datasets (see {@link #ENSDFDataset(java.util.List, double, jgamut.JGAMUT_GUI.updateStatus) 
     * ENSDFDataset(List, double, updateStatus)}).
     * @param combinedset combined dataset containing all input gamma rays
     * @param tabSeparated if <code>true</code> then the columns of the table
     * will be separated by tabs instead of spaces
     * @return the lines of the intermediate file
     */
    public static final List<String> generateDatasetTable(ENSDFDataset combinedset,
            boolean tabSeparated){
        textTable result;
        int count;
        level tmp;
        List<gammaEquivalenceClass> groupedGammas;
        
        result = new textTable();
        count = 0;
        for(level l : combinedset.Levels){
            result.setCell(count, 0, trimTail(l.getLvlRecord().toString().substring(0, 39)));
            //annotate which levels came from the adopted file
            if(l.isAdopted()){
                result.appendCell(count, 0, "  **");
            }
            
            count += 1;
            if(l.getGammaOutList().isEmpty()){
                count += 1;
                continue;
            }
            result.setCell(count, 0, "Data set");
            result.setCell(count, 1, "Eg");
            result.setCell(count, 2, "Ig (orig.)");
            result.setCell(count, 3, "Ig (norm.)");
            result.setCell(count, 4, "Init. Level");
            result.setCell(count, 5, "Final Level");
            result.setCell(count, 6, "Mu");
            result.setCell(count, 7, "MR");
            count += 1;
            
            groupedGammas = groupByGamma(l);
            for(gammaEquivalenceClass G : groupedGammas){
                for(gamma g : G.getGammas()){
                    result.setCell(count, 0, g.getSource()); //data set name where the gamma came from
                    if(g.isPlacementUncertain()){ //gamma ray energy with notation from ENSDF column 80
                        result.setCell(count, 1, 
                                dataPt.ENSDFprint(g.getEg(), g.getDEg(), true, true) + "?");
                    }else if(g.isExpectedUnobserved()){
                        result.setCell(count, 1, 
                                dataPt.ENSDFprint(g.getEg(), g.getDEg(), true, true) + "S");
                    }else{
                        result.setCell(count, 1, 
                                dataPt.ENSDFprint(g.getEg(), g.getDEg(), true, true));
                    }
                    if(g.getIntensitySymbol().equals("@")){
                        result.appendCell(count, 1, g.getIntensitySymbol());
                    }
                    result.setCell(count, 2, g.getIg() + " " + g.getDIg() + g.getIntensitySymbol()); //gamma ray intensity (original)
                    result.setCell(count, 3, dataPt.ENSDFprint(g.getIg(true), g.getDIg(true), true)); //get renormalized intensities
                    tmp = g.getParent(); //initial (parent) level
                    if(tmp != null){
                        result.setCell(count, 4, tmp.getLvlRecord().getE() + " " 
                                + tmp.getLvlRecord().getJ());
                    }
                    tmp = g.getFinalLevel(); //final level
                    if(tmp != null){
                        result.setCell(count, 5, tmp.getLvlRecord().getE() + " " 
                                + tmp.getLvlRecord().getJ());
                    }
                    result.setCell(count, 6, g.getGammaRecord().getM());
                    result.setCell(count, 7, g.getGammaRecord().getMR());
                    count += 1;
                }
                count += 1; //blank line between sets of gamma rays
            }
            count += 1;
        }

        return result.toStringList(tabSeparated);
    }
    
    /**
     * Reads the intermediate file to reproduce the combined ENSDF dataset. 
     * @param input lines of the intermediate file
     * @param ignoreHashedGammas if <code>true</code> then gamma ray energies
     * with a hash (#) in front will not be read, nor will the associated
     * intensity. If only the intensity is marked with a hash then the 
     * energy will still be read, but the intensity will not.
     * @param tabSeparated if <code>true</code> then it will be assumed that
     * the columns of the intermediate file are separated by a tab character
     * instead of a double space.
     * @return the combined ENSDF dataset containing the levels and gammas
     * listed in the intermediate file
     */
    public static final ENSDFDataset readDatasetTable(List<String> input, 
            boolean ignoreHashedGammas, boolean tabSeparated){
        textTable inputTable;
        ENSDFDataset result;
        level finalLevel;
        gamma g;
        String nucid;
        String levelString;
        String finalLevelStringArray[];
        boolean readingGammas;
        int i;
        String source, E, DE, I, DI, Mu, MR, intensitySymbol, column2, column3, column4, column6;
        boolean placementUncertain, expectedUnobserved;
        List<gammaEquivalenceClass> gammaGroups;
        List<String> initialFinalLevelPairs;
        
        result = new ENSDFDataset("", "Data Table Dataset", "");
        inputTable = new textTable(input, 8, tabSeparated); //there should be 8 columns
        
        nucid = "NNNNN";
        readingGammas = false;
        for(i=0; i<inputTable.getnrow(); i++){
            if(inputTable.getCell(i, 0).trim().equals("")){
                //if the first column is empty then the whole row is empty
                //do nothing
            }else if(inputTable.getCell(i, 4).trim().equals("")){
                //if the fifth column (init. level) is empty then this row starts a new level
                //remove "**" annotation which was included to identify which
                //levels came from the adopted dataset
                levelString = trimTail(input.get(i).replace("**", ""));
                nucid = (new ENSDFLevelRecord(levelString)).getNucid();
                
                try{
                    result.addLevel(new level(new ENSDFLevelRecord(levelString), 
                            "Dataset table"), false);
                }catch(NullPointerException e){
                    JOptionPane.showMessageDialog(null, "Intermediate File Error! The following line does not comply with the intermediate file format:\n" +
                            levelString +
                            "\n No output will be produced", "Intermediate File Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                readingGammas = false;
            }else if(inputTable.getCell(i, 0).trim().equals("Data set")){
                //begins the table of gammas for the level
                readingGammas = true;
            }else if(readingGammas){
                //if none of the above hold and we are reading gamma rays then
                //this row defines a gamma ray
                source = inputTable.getCell(i, 0).trim();
                if(source.charAt(0) == '#'){
                    continue; //skip datasets which are "commented out"
                }
                column2 = inputTable.getCell(i, 1).replace("@", "").trim(); //energy column
                placementUncertain = column2.contains("?");
                expectedUnobserved = column2.contains("S");
                column2 = column2.replace("?", "").replace("S", "").trim();
                if(column2.contains(" ")){
                    E = column2.split(" ")[0];
                    DE = column2.split(" ")[1];
                }else{
                    E = column2;
                    DE = "";
                }
                column3 = inputTable.getCell(i, 2).trim(); //original intesity column
                column4 = inputTable.getCell(i, 3).trim(); //normalized intesity column
                
                intensitySymbol = "";
                if (!column3.isEmpty()) {
                    char column3LastChar = column3.charAt(column3.length() - 1);
                    for(int j=0; j<gamma.intensitySymbols.length; j++){
                        if (column3LastChar == gamma.intensitySymbols[j]) {
                            intensitySymbol = String.valueOf(column3LastChar);
                            column3 = column3.substring(column3.length() - 1);
                        }
                    }
                }
                
                if(column3.equals("")){
                    I = "";
                    DI = "";
                }else if(!column3.contains(" ")){
                    I = column3;
                    DI = "";
                }else{
                    I = column3.split(" ")[0];
                    DI = column3.split(" ")[1];
                }
                if(E.contains("#") && ignoreHashedGammas){
                    continue; //ignore gammas that are "commented out"
                }
                if(I.contains("#") && ignoreHashedGammas){
                    I = ""; //ignore intensities that are "commented out"
                    DI = "";
                }
                column6 = inputTable.getCell(i, 5).trim(); //final level column
                finalLevelStringArray = column6.split(" ", 2);
                finalLevel = result.getLevel(finalLevelStringArray[0]);
                if(finalLevel == null){
                    JOptionPane.showMessageDialog(null, "Intermediate File Error! No final level matches " + finalLevelStringArray[0] + "." +
                            "\nNo output will be produced.", "Intermediate File Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                
                Mu = inputTable.getCell(i, 6).trim();
                MR = inputTable.getCell(i, 7).trim();
                g = new gamma(E, DE, I, DI, expectedUnobserved, 
                        placementUncertain, source, Mu, MR);
                g.getGammaRecord().setNucid(nucid);
                g.setIntensitySymbol(intensitySymbol);
                result.addGamma(g, result.getLastLevel());
                result.getLastGamma().setFinalLevel(finalLevel);
                if(column4.equals("")){
                    I = "";
                    DI = "";
                }else if(!column4.contains(" ")){
                    I = column4;
                    DI = "";
                }else{
                    I = column4.split(" ")[0];
                    DI = column4.split(" ")[1];
                }
                result.getLastGamma().setIg_renorm(I);
                result.getLastGamma().setDIg_renorm(DI);
            }
        }
        
        //check that no two gamma rays from the same dataset have the same
        //initial and final level. That would indicate the same gamma occured
        //twice in a dataset
        initialFinalLevelPairs = new ArrayList<>();
        for(level l : result.Levels){
            gammaGroups = groupByGamma(l);
            for(gammaEquivalenceClass G : gammaGroups){
                if(G.isEmpty()){
                    continue;
                }
                g = G.getGammas().get(0);
                try{
                    levelString = g.getParent().getLvlRecord().getE() + 
                            " " + g.getFinalLevel().getLvlRecord().getE();
                }catch(NullPointerException e){
                    continue;
                }
                if(initialFinalLevelPairs.contains(levelString)){
                    JOptionPane.showMessageDialog(null, "Intermediate File Error! Two gammas from the same dataset have the same initial and final levels.\nSee level: " +
                            g.getParent().getLvlRecord().getE() + " " + g.getParent().getLvlRecord().getJ() + 
                            "\nNo output will be produced.",
                            "Intermediate File Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }else{
                    initialFinalLevelPairs.add(levelString);
                }
            }
        }
        
        return result;
    }
    /**
     * Calls <code>{@link #readDatasetTable(java.util.List, boolean, boolean) 
     * readDatasetTable(input, false, tabSeparated)}</code>.
     * @param input lines of the intermediate file
     * @param tabSeparated if <code>true</code> then it will be assumed that
     * the columns of the intermediate file are separated by a tab character
     * instead of a double space
     * @return the combined ENSDF dataset containing the levels and gammas
     * listed in the intermediate file
     */
    public static final ENSDFDataset readDatasetTable(List<String> input, 
            boolean tabSeparated){
        return readDatasetTable(input, false, tabSeparated);
    }
    
    /**
     * Computes the average gamma-ray energy and intensity for each group of
     * gammas that are the same, but from different measurements. As a side
     * effect, the average gamma-ray for each group is added to the group that
     * it is the average of.
     * @param gammaGroups List of groups of gammas. Each group represents a single
     * unique gamma-ray and contains all measurements of that gamma-ray
     * @param limitMinUncert if <code>true</code> then the uncertainty on the
     * averages will not be lower than the lowest measurement which contributed
     * to the average
     * @param useNonNumericUncert if <code>true</code> then the unweighted
     * average will use all measurements, even those with non-numeric uncertainties.
     * The weighted average and NRM procedures always ignore non-numeric uncertainties.
     * @param decayData <code>true</code> if the data should be normalized like
     * a decay dataset (as opposed to an Adopted Levels, Gammas dataset
     * @return the lines of the averages file produced by the "gamma-by-gamma"
     * approach.
     */
    public static final List<String> generateAveragesTable(List<gammaEquivalenceClass> gammaGroups, 
            boolean limitMinUncert, boolean useNonNumericUncert, boolean decayData){
        textTable result;
        gammaEquivalenceClass G;
        gamma g,G_av;
        int i, count;
        String energy, intensity;
        ENSDFDataset averageDataset;
        boolean hasLevel;
        level p, f;
        
        g = null;
        for(i=0; i<gammaGroups.size(); i++){
            G = gammaGroups.get(i);
            if(G.isEmpty()){
                continue;
            }
            g = G.getGammas().get(0);
            break;
        }
        try{
            averageDataset = new ENSDFDataset(g.getGammaRecord().getNucid(), "AVERAGE", "");
        }catch(NullPointerException e){
            //there are no gammas...
            return new ArrayList<>();
        }
        
        for(i=0; i<gammaGroups.size(); i++){
            G = gammaGroups.get(i);
            G.clearUnresolvedIntensities();
            if(G.isEmpty()){
                continue;
            }
            g = G.getGammas().get(0);
            p = null;
            f = null;
            if(averageDataset.Levels.isEmpty()){
                averageDataset.addLevel(g.getParent());
                p = averageDataset.getLastLevel();
                if(g.getFinalLevel() != null){
                    averageDataset.addLevel(g.getFinalLevel());
                    f = averageDataset.getLastLevel();
                }
            }else{
                hasLevel = false;
                //check if parent level is already in the dataset
                for(level l : averageDataset.Levels){
                    if(l.equals(g.getParent())){
                        hasLevel = true;
                        p = l;
                        break;
                    }
                }
                if(!hasLevel){
                    averageDataset.addLevel(g.getParent());
                    p = averageDataset.getLastLevel();
                }
                //check if final level is already in the dataset
                if(g.getFinalLevel() != null){
                    hasLevel = false;
                    for(level l : averageDataset.Levels){
                        if(l.equals(g.getFinalLevel())){
                            hasLevel = true;
                            f = l;
                            break;
                        }
                    }
                    if(!hasLevel){
                        averageDataset.addLevel(g.getFinalLevel());
                        f = averageDataset.getLastLevel();
                    }
                }
            }

            //at this stage both the parent and final levels of g are in
            //the dataset and the pointers p and f have been assigned
            G_av = G.average(limitMinUncert, useNonNumericUncert);
            averageDataset.addGamma(G_av, p);
            if(f != null){
                averageDataset.getLastGamma().setFinalLevel(f);
            }
        }
        //ensure that, for each level, most intense gamma is 100
        averageDataset.renormalizeGammaRays(decayData);
        
        result = new textTable();
        count = 0;
        for(i=0; i<gammaGroups.size(); i++){
            G = gammaGroups.get(i);
            if(G.isEmpty()){
                continue;
            }
            if(i == 0){
                result.setCell(count, 0, G.getLevelString());
                count += 1;
                result.setCell(count, 0, "Average Eg");
                result.setCell(count, 1, "Eg Chi^2");
                result.setCell(count, 2, "Average Ig");
                result.setCell(count, 3, "Ig Chi^2");
                result.addHrule(count);
                count += 1;
            }else if( !G.getLevelString().equals(gammaGroups.get(i-1).getLevelString()) ){
                result.addHrule(count);
                count += 1; //blank line between levels
                result.setCell(count, 0, G.getLevelString());
                count += 1;
                result.setCell(count, 0, "Average Eg");
                result.setCell(count, 1, "Eg Chi^2");
                result.setCell(count, 2, "Average Ig");
                result.setCell(count, 3, "Ig Chi^2");
                result.addHrule(count);
                count += 1;
            }
            g = G.getGammas().get(0);
            G_av = averageDataset.getGamma(g.getParent(), g.getFinalLevel());
            //G_av = G.average(limitMinUncert, useNonNumericUncert);
            if(G_av == null){
                G_av = G.average(limitMinUncert, useNonNumericUncert);
            }
            G.add(G_av);
            energy = G_av.getEg() + " " + G_av.getDEg();
            if(!(G_av.energyAveragingMethod.equals("WtAve") || 
                    G_av.energyAveragingMethod.equals(""))){
                energy += "  *" + G_av.energyAveragingMethod + "*";
            }
            intensity = G_av.getIg(true) + " " + G_av.getDIg(true);
            
            
            if(!(G_av.intensityAveragingMethod.equals("WtAve") || 
                    G_av.intensityAveragingMethod.equals(""))){
                intensity += "  *" + G_av.intensityAveragingMethod + "*";
            }
            result.setCell(count, 0, energy);
            result.setCell(count, 1, G_av.energyChiSq);
            result.setCell(count, 2, intensity);
            result.setCell(count, 3, G_av.intensityChiSq);
            count += 1;
        }
        
        return result.toStringList();
    }
    
    /**
     * Generates the averages file produced after the GAMUT method.
     * @param orig the combined dataset read from the intermediate file with no
     * adjustments made
     * @param modified the combined dataset read of the intermediate file with
     * any modifications made by the user during the GAMUT chi^2 analysis.
     * @param adopted the dataset produced by running the GAMUT algorithms on
     * the combined dataset read from the intermediate file
     * @param beta the fitted scale factors from the GAMUT intensity algorithm
     * @param energyShifts the fitted systematic energy shifts from the
     * GAMUT energy algorithm
     * @param dEnergyShifts uncertainty in the fitted systematic energy shifts from the
     * GAMUT energy algorithm
     * @return the lines of the averages file produced after the GAMUT method
     */
    public static final List<String> generateReportTable(ENSDFDataset orig,
            ENSDFDataset modified, ENSDFDataset adopted, Matrix beta,
            Matrix energyShifts, Matrix dEnergyShifts){
        textTable result;
        int linecount, lvlcount, gammacount;
        level tmp;
        List<gammaEquivalenceClass> groupedGammas;
        int i, j;
        String lvlString;
        level[] modLevels = modified.getLevelsWithGammas();
        boolean modBoolE, modBoolI;
        double chiSq;
        double[] datasetChiSqsE, datasetChiSqsI;
        int[] dataSetGammaCount;
        int datasetIndex;
        List<String> sources = java.util.Arrays.asList(orig.getSources());
        
        datasetChiSqsE = new double[sources.size()];
        datasetChiSqsI = new double[sources.size()];
        dataSetGammaCount = new int[sources.size()];
        for(i=0; i<datasetChiSqsE.length; i++){
            datasetChiSqsE[i] = 0.0d;
            datasetChiSqsI[i] = 0.0d;
            dataSetGammaCount[i] = 0;
        }
        result = new textTable();
        linecount = datasetChiSqsE.length + 5; //leave room for the summary
        lvlcount = 0;
        for(level l : orig.Levels){
            if(!l.hasNonAdoptedGammas()){
                lvlString = trimTail(l.getLvlRecord().toString().substring(0, 39));
                result.setCell(linecount, 0, "***" + lvlString + "*** IGNORED");
                linecount += 2;
                continue;
            }
            lvlString = trimTail(adopted.Levels.get(lvlcount).getLvlRecord()
                    .toString().substring(0, 39));
            result.setCell(linecount, 0, lvlString);
            
            linecount += 1;
            if(l.getGammaOutList().isEmpty()){
                linecount += 1;
                lvlcount += 1;
                continue;
            }
            result.setCell(linecount, 0, "Data set");
            result.setCell(linecount, 1, "Eg");
            result.setCell(linecount, 2, "Ig");
            result.setCell(linecount, 3, "Ig (norm.)");
            result.setCell(linecount, 4, "Chi^2 (Eg)");
            result.setCell(linecount, 5, "Chi^2 (Ig)");
            result.setCell(linecount, 6, "Init. Level");
            result.setCell(linecount, 7, "Final Level");
            linecount += 1;
            
            groupedGammas = groupByGamma(modLevels[lvlcount]);
            gammacount = 0;
            for(i=0; i<groupedGammas.size(); i++){
                if(groupedGammas.get(i).isEmpty()){
                    continue;
                }
                //remove adopted gamma (if any)
                //the adopted gamma will be the first one since that is how
                //the gammaEquivalenceClass sorting works
                if(groupedGammas.get(i).getGammas().get(0).isAdopted()){
                    groupedGammas.get(i).getGammas().remove(0);
                }
                if(groupedGammas.get(i).isEmpty()){
                    continue;
                }
                //insert gammas from GAMUT fit
                try{
                    groupedGammas.get(i).add(adopted.Levels.get(lvlcount).getOutGRay(gammacount));
                }catch(IndexOutOfBoundsException e){
                    JOptionPane.showMessageDialog(null, "Error! " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                groupedGammas.get(i).sort();
                gammacount += 1;
            }
            for(gammaEquivalenceClass G : groupedGammas){
                gamma gamma0;
                if (G.getGammas().isEmpty()) {
                    continue;
                } else {
                    gamma0 = G.getGammas().get(0);
                }
                for(gamma g : G.getGammas()){
                    if (g == null) {
                        continue;
                    }
                    result.setCell(linecount, 0, g.getSource()); //data set name where the gamma came from
                    datasetIndex = sources.indexOf(g.getSource());
                    modBoolE = false;
                    modBoolI = false;
                    if(!g.getSource().contains("GAMUT")){
                        for(gamma g_orig : l.getOutGRays()){ //check if uncertainties were modified
                            if(g.getSource().equals(g_orig.getSource())){
                                if(g.getFinalLevel().equals(g_orig.getFinalLevel())){
                                    modBoolE = !g.getDEg().equals(g_orig.getDEg());
                                    modBoolI = !g.getDIg().equals(g_orig.getDIg());
                                    break;
                                }
                            }
                        }
                    }
                    if(modBoolE){
                        result.setCell(linecount, 1, 
                                "*" + dataPt.ENSDFprint(g.getEg(), g.getDEg(), true) + 
                                        "*");
                    }else{
                        result.setCell(linecount, 1, 
                                dataPt.ENSDFprint(g.getEg(), g.getDEg(), true));
                    }
                    
                    if(modBoolI){
                        result.setCell(linecount, 2, 
                                "*" + dataPt.ENSDFprint(g.getIg(), g.getDIg(), true) + 
                                        "*");
                    }else{
                        result.setCell(linecount, 2, 
                                dataPt.ENSDFprint(g.getIg(), g.getDIg(), true));
                    }
                    
                    result.setCell(linecount, 3, dataPt.ENSDFprint(g.getIg(true), g.getDIg(true), true)); //get renormalized intensities
                    if(!g.getSource().contains("GAMUT")){ //chi^2 calculated wrt GMAUT gamma ray
                        //set Eg chi^2
                        if (g.eg.isPresent() && gamma0.eg.isPresent()) {
                            dataPt dp = dataPt.constructFromString(g.getEg() + " " + g.getDEg());
                            double uncert;
                            if (dp == null) {
                                uncert = 0d;
                                // don't proceed with chi^2 calculation, it does not make sense
                            } else {
                                uncert = dp.getLower();
                                if(energyShifts != null){
                                    chiSq = (g.eg.get() + 
                                        energyShifts.get(datasetIndex, 0) - 
                                        gamma0.eg.get())/uncert;
                                }else{
                                    chiSq = (g.eg.get() - gamma0.eg.get())/uncert;
                                }
                                chiSq *= chiSq;
                                datasetChiSqsE[datasetIndex] += chiSq;
                                dataSetGammaCount[datasetIndex] += 1;
                                result.setCell(linecount, 4, String.format("%1.4f", chiSq));
                            }
                        }
                        //set Ig chi^2
                        //skip gamma rays without normalized uncertainty
                        //(probably only gamma from that level, not fitted)
                        if(!g.getDIg(true).equals("") && g.ig.isPresent() && gamma0.ig.isPresent()) {
                            dataPt dp = dataPt.constructFromString(g.getIg() + " " + g.getDIg());
                            double uncert;
                            if (dp == null) {
                                uncert = 0d;
                                // don't proceed with chi^2 calculation, it does not make sense
                            } else {
                                uncert = dp.getLower();
                                if(beta != null){
                                    chiSq = (g.ig.get() - 
                                        beta.get(datasetIndex, 0)*gamma0.ig.get())/
                                        uncert;
                                }else if(modLevels[lvlcount].beta != null){
                                    tmp = modLevels[lvlcount];
                                    chiSq = (g.ig.get() - 
                                        tmp.beta[Arrays.asList(tmp.gammaSources).indexOf(g.getSource())]*
                                            gamma0.ig.get())/uncert;
                                }else{
                                    chiSq = (g.ig.get() - gamma0.ig.get())/uncert;
                                }
                                chiSq *= chiSq;
                                datasetChiSqsI[datasetIndex] += chiSq;
                                if (!g.eg.isPresent()) {
                                    //add to the dataset's gamma count if it 
                                    //was not done already when calculating the
                                    //energy chi^2
                                    dataSetGammaCount[datasetIndex] += 1;
                                }
                                result.setCell(linecount, 5, String.format("%1.4f", chiSq));
                            }
                        }
                    }
                    
                    tmp = g.getParent(); //initial (parent) level
                    if(tmp != null){
                        result.setCell(linecount, 6, tmp.getLvlRecord().getE() + " " 
                                + tmp.getLvlRecord().getJ());
                    }
                    tmp = g.getFinalLevel(); //final level
                    if(tmp != null){
                        result.setCell(linecount, 7, tmp.getLvlRecord().getE() + " " 
                                + tmp.getLvlRecord().getJ());
                    }
                    linecount += 1;
                }
                linecount += 1; //blank line between sets of gamma rays
            }
            linecount += 1;
            lvlcount += 1;
        }
        linecount = 0;
        result.setCell(linecount, 0, "SUMMARY");
        result.addHrule(linecount);
        linecount += 1;
        result.setCell(linecount, 0, "Dataset");
        result.setCell(linecount, 1, "Average Eg Chi^2");
        result.setCell(linecount, 2, "Average Ig Chi^2");
        if(beta != null){
            result.setCell(linecount, 3, "Intensity Mult.");
        }
        if(energyShifts != null){
            result.setCell(linecount, 4, "Energy Shift");
        }
        linecount += 1;
        for(i=0; i<datasetChiSqsE.length; i++){
            result.setCell(linecount, 0, sources.get(i));
            if(dataSetGammaCount[i] > 0){
                result.setCell(linecount, 1, String.format("%1.4e", 
                    datasetChiSqsE[i]/(double)dataSetGammaCount[i]));
                result.setCell(linecount, 2, String.format("%1.4e", 
                    datasetChiSqsI[i]/(double)dataSetGammaCount[i]));
                if(beta != null){
                    result.setCell(linecount, 3, String.format("%1.4e", 
                        1.0d/beta.get(i, 0)));
                }
                if(energyShifts != null){
                    try{
                        result.setCell(linecount, 4, 
                                (new dataPt(energyShifts.get(i, 0), 
                                        dEnergyShifts.get(i, 0), 
                                        dEnergyShifts.get(i, 0))).toString());
                    }catch(NumberFormatException e){
                        result.setCell(linecount, 4, 
                                String.valueOf(energyShifts.get(i, 0)) + " 0");
                    }
                }
            }else{
                result.setCell(linecount, 1, "-");
                result.setCell(linecount, 2, "-");
                if(beta != null){
                    result.setCell(linecount, 3, "-");
                }
                if(energyShifts != null){
                    result.setCell(linecount, 4, "-");
                }
            }
            linecount += 1;
        }
        result.addHrule(linecount);
        
        return result.toStringList();
    }
    
    /**
     * Replaces the numbers for the gamma-ray energies and intensities in the
     * original ADOPTED LEVELS, GAMMAS dataset with the new values obtained by
     * using the gamma-by-gamma averaging approach.
     * @param gammaGroups the groups of gammas passed to <code>
     * {@link #generateAveragesTable(java.util.List, boolean, boolean, boolean) 
     * generateAveragesTable}</code>. Note that as a side effect <code>
     * {@link #generateAveragesTable(java.util.List, boolean, boolean, boolean) 
     * generateAveragesTable}</code> adds the average gamma-ray to each group.
     * @param orig_adopted the lines from the original ADOPTED LEVELS, GAMMAS
     * dataset
     * @return the lines of the new ADOPTED LEVELS, GAMMAS dataset, with the
     * numbers for the gamma-ray energies and intensities replaced.
     */
    public static final List<String> generateAdoptedDataset(List<gammaEquivalenceClass> gammaGroups,
            ENSDFDataset orig_adopted){
        List<String> result = new ArrayList();
        String rType;
        ENSDFGammaRecord gamRec;
        boolean foundGamma;
        int i;
        String s;
        gamma gam;
        
        for(gammaEquivalenceClass G : gammaGroups){
            for(gamma g : G.getGammas()){
                if(g.getSource().toLowerCase().contains("average")){
                    g.matched = false;
                }
            }
        }
        
        for(String line : orig_adopted.Lines){
            rType = ENSDFRecord.getCol(line, 6, 8);
            
            //only modify the gamma records
            if(rType.equals("G")){
                gamRec = new ENSDFGammaRecord(line);
                //search for the matching gamma ray in one of the 
                //gamma equivalence classes
                foundGamma = false;
                for(gammaEquivalenceClass G : gammaGroups){
                    for(gamma g : G.getGammas()){
                        if(g.isAdopted()){
                            //only check that NICID, E, DE, RI, DRI, M, MR and Q
                            //are the same when checking equality.
                            if(g.partialEquals(gamRec)){
                                //change the energy and intensity to match
                                //those from the average of this group
                                for(gamma g2 : G.getGammas()){
                                    if(g2.getSource().toLowerCase()
                                            .contains("average")){
                                        gamRec.setE(g2.getEg());
                                        gamRec.setDE(g2.getDEg());
                                        gamRec.setRI(g2.getIg(true));
                                        gamRec.setDRI(g2.getDIg(true));
                                        g2.matched = true;
                                        foundGamma = true;
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if(foundGamma){
                        break;
                    }
                }
                result.add(gamRec.toString());
            }else{
                result.add(line);
            }
        }
        
        //look for new gamma rays which were not in the original adopted
        //data set
        for(gammaEquivalenceClass G : gammaGroups){
            for(gamma g : G.getGammas()){
                if(g.getSource().toLowerCase().contains("average")){
                    if(!g.matched){
                        //insert the new gamma
                        for(i=0; i<result.size(); i++){
                            s = result.get(i);
                            rType = ENSDFRecord.getCol(s, 6, 8);
                            if(rType.equals("L")){
                                if(trimTail(ENSDFRecord.getCol(s, 1, 39)).equals(G.getLevelString())){
                                    //found the parent level, now we need to add it in
                                    //the right place (maintaining the energy-sorted
                                    //gamma ray order)
                                    OUTER:
                                    while (true) {
                                        i+= 1;
                                        s = result.get(i);
                                        rType = ENSDFRecord.getCol(s, 6, 8);
                                        switch (rType) {
                                            case "G":
                                                gam = new gamma(new ENSDFGammaRecord(s), orig_adopted.title);
                                                if (gamma.energyComparator.compare(g, gam) < 0) {
                                                    result.add(i, g.getGammaRecord().toString());
                                                    break OUTER;
                                                }
                                                break;
                                            case "L":
                                                //reached next level, need to add gamma ray now
                                                result.add(i, g.getGammaRecord().toString());
                                                break OUTER;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Generates a skeleton ADOPTED LEVELS, GAMMAS dataset from the gamma-by-gamma
     * averages.
     * @param gammaGroups the groups of gammas passed to <code>
     * {@link #generateAveragesTable(java.util.List, boolean, boolean, boolean) 
     * generateAveragesTable}</code>. Note that as a side effect <code>
     * {@link #generateAveragesTable(java.util.List, boolean, boolean, boolean) 
     * generateAveragesTable}</code> adds the average gamma-ray to each group.
     * @return skeleton ADOPTED LEVELS, GAMMAS dataset from the gamma-by-gamma
     * averages
     */
    public static final List<String> generateAdoptedDataset(List<gammaEquivalenceClass> gammaGroups){
        String nucid;
        List<String> result = new ArrayList<>();
        int i;
        
        nucid = "";
        for(gammaEquivalenceClass G : gammaGroups){
            if(!G.getLevelString().equals("")){
                nucid = (new ENSDFLevelRecord(G.getLevelString())).getNucid();
                break;
            }
        }
        
        //ID record
        result.add(nucid + "    ADOPTED LEVELS, GAMMAS                                                 ");
        //first level record
        result.add(gammaGroups.get(0).getLevelString());
        if(!gammaGroups.get(0).isEmpty()){
            for(gamma g : gammaGroups.get(0).getGammas()){
                //add average gamma records
                if(g.getSource().toLowerCase().contains("average")){
                    g.getGammaRecord().setNucid(nucid);
                    result.add(g.getGammaRecord().toString());
                    break;
                }
            }
        }
        
        //add other level and average gamma records
        for(i=1; i<gammaGroups.size(); i++){
            if(!gammaGroups.get(i).getLevelString()
                    .equals(gammaGroups.get(i-1).getLevelString())){
                if(!gammaGroups.get(i).getLevelString().equals("")){
                    result.add(gammaGroups.get(i).getLevelString());
                }
            }
            for(gamma g : gammaGroups.get(i).getGammas()){
                if(g.getSource().toLowerCase().contains("average")){
                    g.getGammaRecord().setNucid(nucid);
                    result.add(g.getGammaRecord().toString());
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Generates a skeleton ADOPTED LEVELS, GAMMAS dataset from the ENSDF dataset
     * produced by the GAMUT algorithms
     * @param data the ENSDF dataset produced by the GAMUT algorithms
     * @return a skeleton ADOPTED LEVELS, GAMMAS dataset
     */
    public static final List<String> generateAdoptedDataset(ENSDFDataset data){
        List<String> result = new ArrayList<>();
        ENSDFGammaRecord gamRec;
        
        //ID record
        result.add(data.nucid + "    ADOPTED LEVELS, GAMMAS                                                 ");
        for(level l : data.Levels){
            result.add(l.getLvlRecord().toString());
            for(gamma g : l.getOutGRays()){
                gamRec = new ENSDFGammaRecord(g.getGammaRecord());
                try{
                    gamRec.setRI(g.getIg(true));
                    gamRec.setDRI(g.getDIg(true));
                }catch(NullPointerException e){
                    gamRec.setRI("");
                    gamRec.setDRI("");
                }
                result.add(gamRec.toString());
            }
        }
        
        return result;
    }
    
    /**
     * Adds additional gamma-rays to the ADOPTED LEVELS, GAMMAS dataset which
     * were not in the original one.
     * @param previousLevelRecordIndex index of the line in ADOPTED LEVELS, 
     * GAMMAS which has the last level record added to
     * ADOPTED LEVELS, GAMMAS
     * @param result the lines of ADOPTED LEVELS, GAMMAS
     * @param gamutLevel the level containing the missed gammas
     * @param gamutGammaIndex the initial index of the gamma-ray which has been missed
     * @param finalGamutGammaIndex the index of the gamma that will be added after
     * the missed gamma-rays are added
     * @return <code>finalGamutGammaIndex</code>
     */
    private static int addMissedGammas(int previousLevelRecordIndex, 
            List<String> result, level gamutLevel, int gamutGammaIndex,
            int finalGamutGammaIndex){
        int i;
        String s;
        gamma gamutGamma, gam;
        ENSDFGammaRecord gamRec;
        
        for(i=previousLevelRecordIndex; i<result.size() &&
                gamutGammaIndex < finalGamutGammaIndex; i++){
            gamutGamma = gamutLevel.getOutGRay(gamutGammaIndex);
            s = result.get(i);
            if(ENSDFRecord.getCol(s, 6, 8).equals("G")){
                gam = new gamma(new ENSDFGammaRecord(s), "");
                if(gamma.energyComparator.compare(gamutGamma, gam) < 0){
                    gamRec = new ENSDFGammaRecord(gamutGamma.getGammaRecord());
                    gamRec.setRI(gamutGamma.getIg(true));
                    gamRec.setDRI(gamutGamma.getDIg(true));
                    result.add(i, gamRec.toString());
                    gamutGammaIndex += 1;
                }
            }
        }
        while(gamutGammaIndex < finalGamutGammaIndex){
            gamutGamma = gamutLevel.getOutGRay(gamutGammaIndex);
            gamRec = new ENSDFGammaRecord(gamutGamma.getGammaRecord());
            try{
                gamRec.setRI(gamutGamma.getIg(true));
                gamRec.setDRI(gamutGamma.getDIg(true));
            }catch(NullPointerException e){
                gamRec.setRI("");
                gamRec.setDRI("");
            }
            result.add(gamRec.toString());
            gamutGammaIndex += 1;
        }
        
        return finalGamutGammaIndex;
    }
    
    /**
     * Adds additional levels to the ADOPTED LEVELS, GAMMAS dataset which
     * were not in the original one. 
     * @param data the GAMUT <code>ENSDFDataset</code>
     * @param result the lines of ADOPTED LEVELS, GAMMAS
     * @param gamutLvlIndex the initial level index to add
     * @param nextLevel the level which will be added after the missed levels 
     * are added
     * @return the index of <code>nextLevel</code>
     */
    private static int addMissedLevels(ENSDFDataset data, List<String> result,
            int gamutLvlIndex, level nextLevel){
        int i, j;
        String s;
        boolean levelAdded;
        level gamutLevel, tmp;
        
        for(i=gamutLvlIndex; i<data.Levels.size() &&
                !data.Levels.get(i).equals(nextLevel); i++){
            gamutLevel = data.Levels.get(i);
            gamutLvlIndex = i+1;
            levelAdded = false;
            for(j=0; j<result.size() && !levelAdded; j++){
                s = result.get(j);
                if(ENSDFRecord.getCol(s, 6, 8).equals("L")){
                    tmp = new level(new ENSDFLevelRecord(s), "");
                    //be sure to add level before one of higher energy
                    if(level.energyComparator.compare(gamutLevel, tmp) < 0){
                        result.add(j, gamutLevel.getLvlRecord().toString());
                        for(gamma g : gamutLevel.getGammaOutList()){
                            j += 1;
                            result.add(j, g.getGammaRecord().toString());
                        }
                        levelAdded = true;
                    }
                }
            }
            if(!levelAdded){
                result.add(gamutLevel.getLvlRecord().toString());
                for(gamma g : gamutLevel.getGammaOutList()){
                    result.add(g.getGammaRecord().toString());
                }
            }
        }
        return gamutLvlIndex;
    }
    
    
    /**
     * Replaces the numbers for the gamma-ray energies and intensities in the
     * original ADOPTED LEVELS, GAMMAS dataset with the new values obtained by
     * the GAMUT algorithms.
     * @param data the ENSDF dataset produced by the GAMUT algorithms
     * @param orig_adopted the lines from the original ADOPTED LEVELS, GAMMAS
     * dataset
     * @return the lines of the new ADOPTED LEVELS, GAMMAS dataset, with the
     * numbers for the gamma-ray energies and intensities replaced.
     */
    public static final List<String> generateAdoptedDataset(ENSDFDataset data,
            ENSDFDataset orig_adopted){
        List<String> result = new ArrayList<>();
        String rType;
        ENSDFLevelRecord lvlRec;
        ENSDFGammaRecord gamRec;
        int adoptedLvlIndex, gamutLvlIndex;
        int adoptedGammaIndex, gamutGammaIndex;
        int previousLevelRecordIndex;
        int i;
        level adoptedLevel, gamutLevel, l;
        gamma gamutGamma, adoptedGamma;
        levelEquivalenceClass L = new levelEquivalenceClass();
        boolean levelAdded;
        
        adoptedLvlIndex = 0;
        gamutLvlIndex = 0;
        adoptedGammaIndex = 0;
        gamutGammaIndex = 0;
        previousLevelRecordIndex = 0;
        adoptedLevel = null;
        gamutLevel = null;
        for(String line : orig_adopted.Lines){
            rType = ENSDFRecord.getCol(line, 6, 8);
            
            switch(rType) {
                case "L":
                    if(gamutLevel != null){
                        if(gamutGammaIndex < gamutLevel.getGammaOutList().size()){
                            //some gammas were not inserted in the previous level
                            //need to finish doing those first, ensuring they
                            //maintain the energy-sorted order
                            gamutGammaIndex = addMissedGammas(previousLevelRecordIndex, result, 
                                                gamutLevel, gamutGammaIndex, 
                                                gamutLevel.getGammaOutList().size());
                        }
                    }
                    adoptedLevel = orig_adopted.Levels.get(adoptedLvlIndex);
                    adoptedGammaIndex = 0;
                    //if the level has no gammas then it did not contribute
                    //to the GAMUT fit
                    if(adoptedLevel.getGammaOutList().isEmpty() && 
                            adoptedLevel.getGammaInList().isEmpty()){
                        result.add(line);
                        previousLevelRecordIndex = result.size() - 1;
                        adoptedLvlIndex += 1;
                        break;
                    }
                    L.clear();
                    L.add(adoptedLevel);
                    levelAdded = false;
                    //look for matching GAMUT level (starting from the last used level)
                    for(i=gamutLvlIndex; i<data.Levels.size(); i++){
                        l = data.Levels.get(i);
                        if(L.belongs(l)){
                            //add any gamut levels that have been missed so far
                            gamutLvlIndex = addMissedLevels(data, result, gamutLvlIndex, l);
                            gamutLevel = l;
                            gamutGammaIndex = 0;
                            gamutLvlIndex += 1;
                            lvlRec = new ENSDFLevelRecord(line);
                            lvlRec.setE(gamutLevel.getLvlRecord().getE());
                            lvlRec.setDE(gamutLevel.getLvlRecord().getDE());
                            result.add(lvlRec.toString());
                            previousLevelRecordIndex = result.size() - 1;
                            levelAdded = true;
                            adoptedLvlIndex += 1;
                            break;
                        }
                    }
                    if(!levelAdded){
                        result.add(line);
                        previousLevelRecordIndex = result.size() - 1;
                        adoptedLvlIndex += 1;
                    }
                    break;
                case "G":
                    if(adoptedLevel == null){
                        //this would happen if there are unplaced gamma rays
                        //since they are listed at the beginning of the
                        //file. We ignore unplaced gammas
                        result.add(line);
                        break;
                    }
                    
                    adoptedGamma = adoptedLevel.getOutGRay(adoptedGammaIndex);
                    L.clear();
                    if(adoptedGamma.getFinalLevel() == null){
                        //could not have been used in the gamut fit
                        result.add(line);
                        adoptedGammaIndex += 1;
                        break;
                    }
                    L.add(adoptedGamma.getFinalLevel());
                    //look for matching gamut gamma
                    for(i=gamutGammaIndex; i<gamutLevel.getGammaOutList().size(); i++){
                        gamutGamma = gamutLevel.getOutGRay(i);
                        if(L.belongs(gamutGamma.getFinalLevel())){
                            //add any missed gammas
                            gamutGammaIndex = addMissedGammas(previousLevelRecordIndex, result, 
                                                gamutLevel, gamutGammaIndex, i);
                            gamRec = new ENSDFGammaRecord(line);
                            gamRec.setE(gamutGamma.getEg());
                            gamRec.setDE(gamutGamma.getDEg());
                            try{
                                gamRec.setRI(gamutGamma.getIg(true));
                                gamRec.setDRI(gamutGamma.getDIg(true));
                            }catch(NullPointerException e){
                                gamRec.setRI("");
                                gamRec.setDRI("");
                            }
                            result.add(gamRec.toString());
                            gamutGammaIndex += 1;
                            adoptedGammaIndex += 1;
                            break;
                        }
                    }
                    
                    break;
                default:
                    result.add(line);
            }
        }
        
        if(gamutLvlIndex < data.Levels.size()){
            //not all the levels were put in
            //add the remaining levels
            gamutLvlIndex = addMissedLevels(data, result, gamutLvlIndex, 
                    data.Levels.get(0));
        }
        
        return result;
    }
}
