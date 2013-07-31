var httpclient = require('ringo/httpclient');

// TODO - make configurable
var oname = 'geneontology';

// TODO - make configurable
var durationInDays = 1;

var today = new Date();
var yesterday = new Date(new Date().setDate(new Date().getDate()-durationInDays));
var range = yesterday.toISOString()+"+TO+"+today.toISOString();
//console.log(range);

//EXAMPLE = "http://sourceforge.net/rest/p/geneontology/ontology-requests/search?q=created_date_dt:[2013-07-30T00:00:00Z%20TO%202013-07-30T23:59:59Z]";
var url = "http://sourceforge.net/rest/p/"+oname+"/ontology-requests/search?q=created_date_dt:["+range+"]";
//console.log(url);
var payload = httpclient.get(url).content;
//console.log(payload);
var resultObj = JSON.parse(payload);
var count = resultObj.count;
//console.log(count);
var tickets = resultObj.tickets;

print(oname+" new tickets for "+range);
print("\n");
var body = "";
for (var k=0; k<tickets.length; k++) {
    var ticket = tickets[k];

    // Example: http://sourceforge.net/p/geneontology/ontology-requests/10193/ cilium/flagellum 
    body += ' * <a href="http://sourceforge.net/p/'+oname+'/ontology-requests/'+ticket.ticket_num+'/">' + ticket.ticket_num + '</a>';
    body += "\t" + ticket.summary + "\n";
}
print(body);

