package danexcodr.ai.pattern;

import java.util.*;

public class Content extends Abstract {

  // terms
  private String T1;
  private String T2;

  // Base terms that originally formed T1/T2, used for contextual unwrapping.
  private Set<String> baseTermsT1 = new HashSet<String>();
  private Set<String> baseTermsT2 = new HashSet<String>();

  private Set<List<String>> observedStructures = new HashSet<List<String>>();

  // ONLY store family ID - NO object reference
  private String familyId = null;

  private boolean isCommutative = false;

  public Content(String id, String T1, String T2) {
    super(id);
    this.T1 = T1;
    this.T2 = T2;
  }

  public Content(String T1, String T2) {
    this("RP" + System.currentTimeMillis(), T1, T2);
  }

  public String getT1() {
    return T1;
  }

  public String getT2() {
    return T2;
  }

  public Set<List<String>> getObservedStructures() {
    return observedStructures;
  }

  // NEW Accessors
  public Set<String> getBaseTermsT1() {
    return baseTermsT1;
  }

  public void setBaseTermsT1(Set<String> terms) {
    this.baseTermsT1.clear();
    this.baseTermsT1.addAll(terms);
    updateTimestamp();
  }

  public Set<String> getBaseTermsT2() {
    return baseTermsT2;
  }

  public void setBaseTermsT2(Set<String> terms) {
    this.baseTermsT2.clear();
    this.baseTermsT2.addAll(terms);
    updateTimestamp();
  }

  // Set family by ID only - NO object reference stored
  public void setFamily(PatternFamily family) {
    this.familyId = (family != null) ? family.getId() : null;
    updateTimestamp();
  }

  // Set family by ID directly
  public void setFamilyId(String familyId) {
    this.familyId = familyId;
    updateTimestamp();
  }

  // Get family ID - IMPORTANT: Make sure this is public
  public String getFamilyId() {
    return this.familyId;
  }

  // Get family by looking it up from current families
  // REQUIRES passing the current families list - NO caching
  public PatternFamily getFamily(List<PatternFamily> currentFamilies) {
    if (familyId == null || currentFamilies == null) return null;

    for (PatternFamily family : currentFamilies) {
      if (family.getId().equals(familyId)) {
        return family;
      }
    }
    return null;
  }

  public void setCommutative(boolean commutative) {
    this.isCommutative = commutative;
    updateTimestamp();
  }

  public boolean isCommutative() {
    return this.isCommutative;
  }

  @Override
  public boolean matchesSequence(List<String> sequence) {
    return sequence.contains(T1) && sequence.contains(T2);
  }
}
