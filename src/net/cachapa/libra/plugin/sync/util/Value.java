package net.cachapa.libra.plugin.sync.util;

public class Value {
	public LDate date;
	public float weight;
	public float weightTrend;
	public float bodyFat;
	public float bodyFatTrend;
	public String comment;
	
	public Value(LDate date, float weight, float weightTrend, float bodyFat, float bodyFatTrend, String comment) {
		this.date = date;
		this.weight = weight;
		this.weightTrend = weightTrend;
		this.bodyFat = bodyFat;
		this.bodyFatTrend = bodyFatTrend;
		this.comment = comment;
	}
	
	public Value(LDate date) {
		this(date, -1, -1, -1, -1, null);
	}
	
	public static Value fromCsv(String csvLine) throws Exception {
		String[] values = csvLine.split(";");
		if (values.length < 2) {
			throw new IllegalArgumentException("Malformed value string: " + csvLine);
		}
		
		Value value = new Value(new LDate(values[0]));
		
		// Weight
		if (values[1].length() > 0) {
			value.weight = Float.valueOf(values[1]).floatValue();
		}
		
		// Body fat
		if (values.length > 3 && values[3].length() > 0) {
			value.bodyFat = Float.valueOf(values[3]).floatValue();
		}
		
		// Comment
		if (values.length > 5 && values[5].length() > 0) {
			value.comment = values[5];
		}
		
		if (value.weight < 0 && value.bodyFat < 0 && value.comment == null) {
			throw new IllegalArgumentException("Malformed value string: " + csvLine);
		}
		return value;
	}
	
	public String toCsv() {
		StringBuilder csvLine = new StringBuilder();
		csvLine.append(date.toString());
		csvLine.append(";");
		csvLine.append((weight < 0) ? "" : weight);
		csvLine.append(";");
		csvLine.append((bodyFat < 0) ? "" : bodyFat);
		csvLine.append(";");
		csvLine.append((comment == null) ? "" : comment);
		
		return csvLine.toString();
	}
}
