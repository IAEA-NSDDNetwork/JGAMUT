
package ENSDF_data;

/**
 * This class implements the ENSDF Gamma Continuation Record (see ENSDF Manual).
 * This is only a partial implementation of the full range of fields which
 * could be specified in the continuation record since only a few data fields
 * are relevant to the JGAMUT code. In particular, the 'FLAG=' and 'FL='
 * entries are parsed, but no other quantities are. Also no data in this
 * record is editable through this class.
 * 
 * Date Modified: 05/07/2015
 * 
 * @author Michael Birch
 */
public class ENSDFGammaContinuationRecord extends ENSDFRecord{
    private final String data;
    private String nucid, FL;
    private char[] FLAG;
    
    /**
     * Creates an <code>ENSDFGammaContinuationRecord</code> object from a 
     * Gamma Continuation record in an ENS file according to the format 
     * defined in the ENSDF manual.
     * @param s ENSDF Gamma Continuation Record string
     */
    public ENSDFGammaContinuationRecord(String s){
        super(s);
        
        String[] tmp;
        int i, j;
        
        this.nucid = super.getNucid();
        this.data = ENSDFRecord.getCol(s, 10, 80);
        
        tmp = this.data.split("$");
        this.FLAG = null;
        this.FL = "";
        for(i=0; i<tmp.length; i++){
            if(tmp[i].contains("FLAG=")){
                tmp[i] = tmp[i].replace("FLAG=", "").trim();
                this.FLAG = new char[tmp[i].length()];
                for(j=0; j<tmp[i].length(); j++){
                    this.FLAG[j] = tmp[i].charAt(j);
                }
            }else if(tmp[i].contains("FL=")){
                tmp[i] = tmp[i].replace("FL=", "").trim();
                this.FL = tmp[i];
            }
        }
    }
    
    /**
     * Copy constructor
     * @param gcr <code>ENSDFGammaContinuationRecord</code> object to copy
     */
    public ENSDFGammaContinuationRecord(ENSDFGammaContinuationRecord gcr){
        this(new String(gcr.record));
    }
    
    //getters
    /**
     * Returns the NUCID for the gamma (see ENSDF manual). Note that whitespace
     * is NOT trimmed.
     * @return the NUCID for the gamma (see ENSDF manual)
     */
    @Override public String getNucid(){
        return this.nucid;
    }
    
    /**
     * Returns the final level energy string specified by 'FL='. IF 'FL='
     * is not present in the continuation record then the string is blank.
     * @return the final level energy string specified by 'FL='
     */
    public String getFL(){
        return this.FL;
    }
    
    /**
     * Returns <code>true</code> when a final level for the gamma was specified
     * via 'FL=' in the continuation record.
     * @return <code>true</code> when a final level for the gamma was specified
     * via 'FL=' in the continuation record.
     */
    public boolean hasFL(){
        return !this.FL.equals("");
    }
    
    /**
     * Returns <code>true</code> when 'FLAG=' has been specified in the
     * continuation record.
     * @return <code>true</code> when 'FLAG=' has been specified in the
     * continuation record.
     */
    public boolean hasFLAG(){
        return this.FLAG != null;
    }
    
    /**
     * Returns <code>true</code> when the character <code>f</code> is one of the
     * flags specified by 'FLAG='.
     * @param f The flag to check is present
     * @return <code>true</code> when the character <code>f</code> is one of the
     * flags specified by 'FLAG='.
     */
    public boolean hasFLAG(char f){
        String s;
        
        if(!hasFLAG()){
            return false;
        }else{
            s = new String(this.FLAG);
            return s.contains(String.valueOf(f));
        }
    }
    
    //setters
    /**
     * Sets the NUCID of the Gamma record (see ENSDF manual).
     * @param s the NUCID of the Gamma record
     */
    public void setNucid(String s){
        this.nucid = s;
        this.fillRecord(s, 1, 5);
    }
}
