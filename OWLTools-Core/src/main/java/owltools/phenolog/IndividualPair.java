package owltools.phenolog;

public class IndividualPair implements Comparable<IndividualPair>{
	
	private Individual member1;
	private Individual member2;


        @Override
        public boolean equals(Object a){
            IndividualPair indpair = (IndividualPair) a;
            String cc1 = this.member1.getId().concat(this.member2.getId());
            String cc2 = indpair.getMember1().getId().concat(indpair.getMember2().getId());
            return cc1.equals(cc2);
        }
 
        @Override
        public int hashCode(){
            return this.member1.getId().hashCode();
        }

        public int compareTo(IndividualPair indpair){
            String cc1 = this.member1.getId().concat(this.member2.getId());
            String cc2 = indpair.getMember1().getId().concat(indpair.getMember2().getId());
            return cc1.compareTo(cc2);
        }        

        public IndividualPair(){
            this.member1 = null;
            this.member2 = null;
        }
        public IndividualPair(Individual member1, Individual member2){
            this.member1 = member1;
            this.member2 = member2;
        }
	public Individual getMember1() {
		return member1;
	}
	public void setMember1(Individual member1) {
		this.member1 = member1;
	}
	public Individual getMember2() {
		return member2;
	}
	public void setMember2(Individual member2) {
		this.member2 = member2;
	}
}
