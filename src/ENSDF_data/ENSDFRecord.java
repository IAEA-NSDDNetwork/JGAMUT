
package ENSDF_data;

/**
 * This is the class used by all the ENSDF Record subclasses
 * (e.g. level and gamma records).
 * 
 * Date Modified: 05/07/2015
 * 
 * @author Michael Birch
 */
public class ENSDFRecord {
    protected char record[];
    
    /**
     * Extracts a substring of <code>s</code> from index <code>lower</code>
     * to index <code>upper</code>. Note: the numbering convention for
     * <code>upper</code> and <code>lower</code> are that of the ENSDF manual
     * so that <code>lower=1</code> will begin the substring from the first
     * character in <code>s</code>. The character at <code>upper</code> is 
     * included in the substring. An attempt to access characters outside 
     * <code>s</code> will return the empty string.
     * The purpose of this method is to extract fields from ENSDF records,
     * e.g. <code>NUCID = getCol(record, 1, 5);</code>.
     * @param s the String to take the substring of.
     * @param lower the starting index of the substring.
     * @param upper the final index of the substring (inclusive).
     * @return a substring of <code>s</code> from index <code>lower</code>
     * to index <code>upper</code>.
     */
    public static final String getCol(String s, int lower, int upper){
        int l, u;
        try{
            return s.substring(lower-1, upper).trim();
        }catch(StringIndexOutOfBoundsException e){
            l = Math.max(0, lower-1);
            u = Math.min(s.length(), upper);
            if(u < l){
                return "";
            }
            return s.substring(l, u).trim();
        }
    }
    /**
     * Returns a string containing the character at position <code>ind</code>
     * of the <code>String s</code>. Equivalent to
     * <code>String.valueOf(s.charAt(ind-1))</code> when <code>1 &le; ind 
     * &le; s.length()</code>, returns a blank String otherwise.
     * @param s The string from which to take the character
     * @param ind The position from which to take the character
     * @return a string containing the character at position <code>ind</code>
     * of the <code>String s</code>.
     */
    public static final String getCol(String s, int ind){
        return getCol(s, ind, ind);
    }
    
    /**
     * Returns a String comprised of characters from index <code>lower-1</code>
     * to index <code>upper-1</code> (inclusive) in the character array 
     * <code>s</code>. An attempt to access characters outside the bounds of
     * the array will return a blank String.
     * @param s The character array from which to make the String
     * @param lower The index of the first character in the returned String
     * @param upper The index of the final character in the returned String
     * @return a String comprised of characters from index <code>lower-1</code>
     * to index <code>upper-1</code> (inclusive) in the character array 
     * <code>s</code>.
     */
    public static final String getCol(char[] s, int lower, int upper){
        int l, u, i;
        char sub[];
        
        l = Math.max(0, lower-1);
        u = Math.min(s.length, upper);
        if(u < l){
            return "";
        }
        
        sub = new char[u-l];
        for(i=l; i<u; i++){
            sub[i-l] = s[i];
        }
        return new String(sub).trim();
    }
    /**
     * Returns a string containing the character at index <code>ind-1</code>.
     * Equivalent to <code>String.valueOf(s[ind-1])</code> when <code>1 &le;
     * ind &le; s.length</code> and returns a blank String otherwise.
     * @param s The character array from which to take the character.
     * @param ind The index of the character to take.
     * @return a string containing the character at index <code>ind-1</code>.
     */
    public static final String getCol(char[] s, int ind){
        int i = ind-1;
        if(i <0 || i > s.length){
            return "";
        }else{
            return String.valueOf(s[i]);
        }
    }
    
    /**
     * Uses the <code>String s</code> to create a char array of length 
     * <code>l</code>. If <code>s.length() &gt;= l</code> then only 
     * the first <code>l</code> characters of <code>s</code> are used.
     * If <code>s</code> is shorter than <code>l</code> then the first
     * element of the returned array is <code>' '</code> and the end of 
     * the returned array is also padded with spaces.
     * This method is used to construct a record from fields that
     * are stored as strings. E.g. the energy field, <code>E</code>, in the 
     * {@link ENSDFLevelRecord} class is stored this way.
     * @param l the length of the char array to fill.
     * @param s the string to fill the array with.
     * @return 
     */
    public static final char[] fillCharArray(int l, String s){
        int i;
        char[] result;
        
        result = new char[l];
        if(l <= s.length()){
            for(i=0; i<l; i++){
                result[i] = s.charAt(i);
            }
        }else{
            result[0] = ' ';
            for(i=1; i<l; i++){
                if(i <= s.length()){
                    result[i] = s.charAt(i-1);
                }else{
                    result[i] = ' ';
                }
            }
        }
        return result;
    }
    
    /**
     * Constructs the <code>ENSDFRecord</code> object from the given
     * <code>String s</code> by filling the 80 character array 
     * <code>record</code> with the characters from <code>s</code> and
     * padding the end with spaces.
     * @param s the string with which to fill the <code>record</code>.
     */
    public ENSDFRecord(String s){
        int i;
        this.record = new char[80];
        
        for(i=0; i<80; i++){
            if(i < s.length()){
                this.record[i] = s.charAt(i);
            }else{
                this.record[i] = ' ';
            }
        }
    }
    
    /** 
     * Returns the NUCID for this record. Note that whitespace is NOT trimmed.
     * @return the record NUCID (see ENDF manual)
     */
    public String getNucid(){
        char[] s;
        
        s = new char[5];
        System.arraycopy(this.record, 0, s, 0, 5); //first 5 characters of the record
        return new String(s);
    }
    
    /** 
     * Returns the type of record (e.g. L, G), see ENSDF manual.
     * @return the type of record (e.g. L, G), see ENSDF manual.
     */
    public String getRecordType(){
        return getCol(this.record, 6, 8);
    }
    
    /**
     * Fills <code>record</code> from <code>start</code> to <code>end</code>
     * with the characters from <code>s</code>. This method uses the 
     * {@link #fillCharArray(int l, String s)} method to change the given string into a
     * character array.
     * @param s String of data with which to fill <code>record</code>.
     * @param start The index to start filling. Note: uses the ENSDF
     * manual numbering convention so that <code>start=1</code> corresponds
     * to the first character in <code>record</code>.
     * @param end The index to fill to also numbered in the ENSDF manual
     * convention.
     */
    protected final void fillRecord(String s, int start, int end){
        char[] tmp;
        int i;
        
        tmp = fillCharArray(end-start+1, s);
        for(i=start-1; i<end; i++){
            this.record[i] = tmp[i-start+1];
        }
    }
    protected final void fillRecord(String s, int ind){
        try{
            this.record[ind-1] = s.charAt(0);
        }catch(StringIndexOutOfBoundsException e){
            this.record[ind-1] = ' ';
        }
    }
    
    /**
     * Returns a string object created from the 80 character array <code>record</code>.
     * @return a string object created from the 80 character array <code>record</code>.
     */
    @Override public String toString(){
        return new String(this.record);
    }
}
