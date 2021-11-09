
package ENSDF_data;


import java.util.ArrayList;
import java.util.List;

/**
 * This class defines the methods used to decide if levels from different
 * datasets are equivalent (ie the same level observed in different 
 * experiments).
 * 
 * Date Modified: 06/07/2015
 * 
 * @author Michael Birch
 */
public class levelEquivalenceClass {
    private List<level> lvls;
    //The indicator used to for this level (see 
    //getTAG documentation in ENSDFLevelRecord).
    private String TAG;
    
    /**
     * Create an empty equivalence class.
     */
    public levelEquivalenceClass(){
        lvls = new ArrayList<>();
        TAG = "";
    }
    
    /**
     * Create equivalence class from a given set of levels.
     * @param levels 
     */
    public levelEquivalenceClass(List<level> levels){
        lvls = levels;
        
        String tag;
        for (level l : levels) {
            tag = l.getLvlRecord().getTAG();
            if (!tag.isEmpty()) {
                TAG = tag;
                break;
            }
        }
        //check the tag is not inconsistent
        for (level l : levels) {
            tag = l.getLvlRecord().getTAG();
            if (!tag.isEmpty() && !tag.equals(TAG)) {
                throw new IllegalArgumentException(
                        "Equivilience class created with inconsistent column 9 tags."
                );
            }
        }
    }
    
    /**
     * Returns <code>true</code> when no levels are in the equivalence class.
     * @return <code>true</code> when no levels are in the equivalence class.
     */
    public boolean isEmpty(){
        return this.lvls.isEmpty();
    }
    
    /**
     * Removes all levels from the class
     */
    public void clear(){
        this.lvls.clear();
    }
    
    /**
     * Returns <code>true</code> when the given level belongs to the
     * equivalence class.
     * @param l2 the level to check belonging
     * @return <code>true</code> when the given level belongs to the
     * equivalence class.
     */
    public boolean belongs(level l2){
        if(this.lvls.isEmpty()){
            return true;
        }
        
        String tag = l2.getLvlRecord().getTAG();
        if (!this.TAG.isEmpty() && !tag.isEmpty()) {
            return this.TAG.equals(tag);
        }
        
        for(level l : lvls){
            //two levels from the same data set cannont be equivalent unless
            //they are the same level
            if(l.getSource().equals(l2.getSource())){
                return false;
            }
            //exact energy string matches definitely belong
            if(l.getLvlRecord().getE().equals(l2.getLvlRecord().getE())){
                return true;
            }
            //exact numerical energy matches belong
            if(Math.abs(l.getEnergy().doubleDiff(l2.getEnergy())) < 1e-20){
                return true;
            }
            if(l.energyMatch(l2)){ //energy of the levels are the same
                //some of the gamma rays coming into/out of the levels
                //are the same
                if(l.gammaMatch(l2) > 0d || l2.gammaMatch(l) > 0d){
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Adds a level to the equivalence class.
     * @param l the level to add
     */
    public void add(level l){
        String tag = l.getLvlRecord().getTAG();
        
        //if TAG is empty, use new level to set it
        if (this.TAG.isEmpty()) {
            if (!tag.isEmpty()) {
                this.TAG = tag;
            }
        //if TAG is not empty, ensure new level is consistent with the TAG
        } else {
            if (!tag.isEmpty()) {
                if (!this.TAG.equals(tag)) {
                    throw new IllegalArgumentException(
                        "Equivilience class created with inconsistent column 9 tags."
                    );
                }
            }
        }
        
        this.lvls.add(l);
    }
    
    /**
     * Returns the list of levels in the equivalence class.
     * @return the list of levels in the equivalence class.
     */
    public List<level> getLevels(){
        return this.lvls;
    }
    
    /**
     * Returns <code>true</code> if a level from 'ADOPTED LEVELS, GAMMAS'
     * is present in the equivalence class.
     * @return <code>true</code> if a level from 'ADOPTED LEVELS, GAMMAS'
     * is present in the equivalence class.
     */
    private boolean containsAdopted(){
        for(level l : this.lvls){
            if(l.isAdopted()){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns one level which represents the equivalence class. This level
     * will be the adopted level it is present, otherwise it will have energy
     * equal to the mean of the energies of the equivalence class and other
     * fields will be blank.
     * @return one level which represents the equivalence class.
     */
    public level toSingleLevel(){
        level result;
        ENSDFEnergy[] energies;
        int i;
        
        result = null;
        if(this.containsAdopted()){
            for(level l : this.lvls){
                if(l.isAdopted()){
                    result = new level(l.getLvlRecord(), l.getSource());
                }
            }
        }else if(this.lvls.size() == 1){
            result = new level(this.lvls.get(0).getLvlRecord(), 
                                this.lvls.get(0).getSource());
        }else{
            energies = new ENSDFEnergy[this.lvls.size()];
            for(i=0; i<energies.length; i++){
                energies[i] = this.lvls.get(i).getEnergy();
            }
            result = new level(this.lvls.get(0).getLvlRecord(), 
                                this.lvls.get(0).getSource());
            result.getLvlRecord().setE(String.format("%1.2f", 
                    ENSDFEnergy.mean(energies).toDouble()));
            result.getLvlRecord().setDE("");
            result.getLvlRecord().setJ("");
        }
        
        return result;
    }
}
