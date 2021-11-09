
package ENSDF_data;

/**
 * This class implements the ENSDF Level Record (see ENSDF Manual).
 * 
 * Date Modified: 05/07/2015
 * 
 * @author Michael Birch
 */
public class ENSDFLevelRecord extends ENSDFRecord{
    private String nucid, E, DE, J, T, DT, L, S, DS, C, MS, Q, TAG;
    
    /**
     * Creates an <code>ENSDFLevelRecord</code> object from a level record
     * in an ENS file according to the format defined in the ENSDF manual.
     * @param s ENSDF Level Record string
     */
    public ENSDFLevelRecord(String s){
        super(s);
        this.nucid = super.getNucid();
        this.TAG = ENSDFRecord.getCol(s, 9);
        this.E = ENSDFRecord.getCol(s, 10, 19);
        this.DE = ENSDFRecord.getCol(s, 20, 21);
        this.J = ENSDFRecord.getCol(s, 22, 39);
        this.T = ENSDFRecord.getCol(s, 40, 49);
        this.DT = ENSDFRecord.getCol(s, 50, 55);
        this.L = ENSDFRecord.getCol(s, 56, 64);
        this.S = ENSDFRecord.getCol(s, 65, 74);
        this.DS = ENSDFRecord.getCol(s, 75, 76);
        this.C = ENSDFRecord.getCol(s, 77);
        this.MS = ENSDFRecord.getCol(s, 78, 79);
        this.Q = ENSDFRecord.getCol(s, 80);
    }
    
    /**
     * Copy constructor
     * @param lr <code>ENSDFLevelRecord</code> object to copy.
     */
    public ENSDFLevelRecord(ENSDFLevelRecord lr){
        this(new String(lr.record));
        
    }
    
    @Override public String getRecordType(){
        return "L";
    }
    
    //getters
    /**
     * Returns the NUCID for the level (see ENSDF manual). Note that whitespace
     * is NOT trimmed.
     * @return the NUCID for the level (see ENSDF manual)
     */
    @Override public String getNucid(){
        return this.nucid;
    }
    
    /**
     * Returns the TAG associated with this level. Column 9 is not used for
     * level records in the ENSDF manual, therefore we have re-purposed it to be
     * a single character indicator for a level across multiple datasets. An
     * evaluator can put the same indicator (e.g.'A') in column 9 for each
     * dataset in which a particular level appears and the code will know that
     * these levels are supposed to be the same (i.e. the matching algorithm
     * will not need to be used).
     * @return TAG associated with the level
     */
    public String getTAG() {
        return this.TAG;
    }
    
    /**
     * Returns the E field of the Level record (see ENSDF manual).
     * @return the E field of the Level record (see ENSDF manual).
     */
    public String getE(){
        return this.E;
    }
    /**
     * Returns the DE field of the Level record (see ENSDF manual).
     * @return the DE field of the Level record (see ENSDF manual).
     */
    public String getDE(){
        return this.DE;
    }
    /**
     * Returns the J field of the Level record (see ENSDF manual).
     * @return the J field of the Level record (see ENSDF manual).
     */
    public String getJ(){
        return this.J;
    }
    /**
     * Returns the T field of the Level record (see ENSDF manual).
     * @return the T field of the Level record (see ENSDF manual).
     */
    public String getT(){
        return this.T;
    }
    /**
     * Returns the DT field of the Level record (see ENSDF manual).
     * @return the DT field of the Level record (see ENSDF manual).
     */
    public String getDT(){
        return this.DT;
    }
    /**
     * Returns the L field of the Level record (see ENSDF manual).
     * @return the L field of the Level record (see ENSDF manual).
     */
    public String getL(){
        return this.L;
    }
    /**
     * Returns the S field of the Level record (see ENSDF manual).
     * @return the S field of the Level record (see ENSDF manual).
     */
    public String getS(){
        return this.S;
    }
    /**
     * Returns the DS field of the Level record (see ENSDF manual).
     * @return the DS field of the Level record (see ENSDF manual).
     */
    public String getDS(){
        return this.DS;
    }
    /**
     * Returns the C field of the Level record (see ENSDF manual).
     * @return the C field of the Level record (see ENSDF manual).
     */
    public String getC(){
        return this.C;
    }
    /**
     * Returns the MS field of the Level record (see ENSDF manual).
     * @return the MS field of the Level record (see ENSDF manual).
     */
    public String getMS(){
        return this.MS;
    }
    /**
     * Returns the Q field of the Level record (see ENSDF manual).
     * @return the Q field of the Level record (see ENSDF manual).
     */
    public String getQ(){
        return this.Q;
    }
    
    //setters
    /**
     * Sets the NUCID of the Level record (see ENSDF manual).
     * @param s the NUCID of the Level record
     */
    public void setNucid(String s){
        this.nucid = s;
        setRecord();
    }
    /**
     * Sets the E field of the Level record (see ENSDF manual).
     * @param s the E field of the Level record (see ENSDF manual).
     */
    public void setE(String s){
        this.E = s;
        setRecord();
    }
    /**
     * Sets the DE field of the Level record (see ENSDF manual).
     * @param s the DE field of the Level record (see ENSDF manual).
     */
    public void setDE(String s){
        this.DE = s;
        setRecord();
    }
    /**
     * Sets the J field of the Level record (see ENSDF manual).
     * @param s the J field of the Level record (see ENSDF manual).
     */
    public void setJ(String s){
        this.J = s;
        setRecord();
    }
    /**
     * Sets the T field of the Level record (see ENSDF manual).
     * @param s the T field of the Level record (see ENSDF manual).
     */
    public void setT(String s){
        this.T = s;
        setRecord();
    }
    /**
     * Sets the DT field of the Level record (see ENSDF manual).
     * @param s the DT field of the Level record (see ENSDF manual).
     */
    public void setDT(String s){
        this.DT = s;
        setRecord();
    }
    /**
     * Sets the L field of the Level record (see ENSDF manual).
     * @param s the L field of the Level record (see ENSDF manual).
     */
    public void setL(String s){
        this.L = s;
        setRecord();
    }
    /**
     * Sets the S field of the Level record (see ENSDF manual).
     * @param s the S field of the Level record (see ENSDF manual).
     */
    public void setS(String s){
        this.S = s;
        setRecord();
    }
    /**
     * Sets the DS field of the Level record (see ENSDF manual).
     * @param s the DS field of the Level record (see ENSDF manual).
     */
    public void setDS(String s){
        this.DS = s;
        setRecord();
    }
    /**
     * Sets the C field of the Level record (see ENSDF manual).
     * @param s the C field of the Level record (see ENSDF manual).
     */
    public void setC(String s){
        this.C = s;
        setRecord();
    }
    /**
     * Sets the MS field of the Level record (see ENSDF manual).
     * @param s the MS field of the Level record (see ENSDF manual).
     */
    public void setMS(String s){
        this.MS = s;
        setRecord();
    }
    /**
     * Sets the Q field of the Level record (see ENSDF manual).
     * @param s the Q field of the Level record (see ENSDF manual).
     */
    public void setQ(String s){
        this.Q = s;
        setRecord();
    }
    
    /**
     * Fills the 80 character array representing the Level record using the
     * data from the various fields.
     */
    private void setRecord(){
        int i;
        
        for(i=0; i<5; i++){
            this.record[i] = this.nucid.charAt(i);
        }
        this.record[5] = ' ';
        this.record[6] = ' ';
        this.record[7] = 'L';
        this.record[8] = ' ';
        
        super.fillRecord(this.E, 10, 19);
        super.fillRecord(this.DE, 20, 21);
        super.fillRecord(this.J, 22, 39);
        super.fillRecord(this.T, 40, 49);
        super.fillRecord(this.DT, 50, 55);
        super.fillRecord(this.L, 56, 64);
        super.fillRecord(this.S, 65, 74);
        super.fillRecord(this.DS, 75, 76);
        super.fillRecord(this.C, 77);
        super.fillRecord(this.MS, 78, 79);
        super.fillRecord(this.Q, 80);
    }
    
    /** Returns the 80 character array <code>record</code> as a string object.
     *
     * @return the 80 character array  <code>record</code> as a string object.
     */
    @Override public String toString(){
        return new String(this.record);
    }
    
    /**
     * Returns <code>true</code> when all the properties of both Level
     * Records are the same.
     * @param other the ENSDFLevelRecord to compare with.
     * @return <code>true</code> when all the properties of both Level
     * Records are the same.
     */
    public boolean equals(ENSDFLevelRecord other){
        boolean result = true;
        result = result && nucid.equals(other.nucid);
        result = result && E.equals(other.E);
        result = result && DE.equals(other.DE);
        result = result && J.equals(other.J);
        result = result && T.equals(other.T);
        result = result && DT.equals(other.DT);
        result = result && L.equals(other.L);
        result = result && S.equals(other.S);
        result = result && DS.equals(other.DS);
        result = result && C.equals(other.C);
        result = result && MS.equals(other.MS);
        result = result && Q.equals(other.Q);
        
        return result;
    }
}
