var Parser = require('ringo/args').Parser;
var httpclient = require('ringo/httpclient');
var system = require('system');

// TODO add option to print to a separate file.
function main(args) {
	var script = args.shift();
	var parser = new Parser(system.args);
	parser.addOption('o', 'ontologyname', 'ONTOLOGY', 'ontology name (or sf project), defaults to "geneontology"');
	parser.addOption('t', 'trackername', 'TRACKER', 'tracker name, defaults to "ontology-requests"');
	parser.addOption('r', 'range', 'RANGE', 'date range, defaults to one day');
	parser.addOption('h', 'help', null, 'Display help');

	var options = parser.parse(args);
	if (options.help) {
		print(parser.help());
		system.exit('-1');
	}
	
	var oname = 'geneontology';
	if (options.ontologyname !== undefined) {
		oname = options.ontologyname;
	}

	var trackername = 'ontology-requests';
	if (options.trackername !== undefined) {
		trackername = options.trackername;
	}
	
	var durationInDays = 1;
	if (options.range !== undefined) {
		durationInDays = parseInt(options.range);
	}
	
	var today = new Date();
	var yesterday = new Date(new Date().setDate(new Date().getDate()-durationInDays));
	var range = yesterday.toISOString()+"+TO+"+today.toISOString();
	//console.log(range);
	
	var newTickets = getNewTickets(oname, trackername, range);
	var modTickets = getUpdatedTickets(oname, trackername, range);
	
	print("<h2>Summary for tickets from "+yesterday.toISOString()+" to "+today.toISOString()+"</h2>");
	printNewTickets(newTickets, oname);
	printUpdatedTickets(modTickets, oname);
	
}

function printNewTickets(tickets, oname) {
	printTickets(tickets, oname, 'new', 'New');
}

function printUpdatedTickets(tickets, oname) {
	printTickets(tickets, oname, 'updated', 'Updated');
}

function printTickets(tickets, oname, type, typeUpperCase) {
	print("<h3>"+typeUpperCase+" Tickets</h3>");
	if (tickets !== undefined && tickets.length > 0) {
		
		if (tickets.length === 1) {
			print("There is one "+type+" ticket.");
		}
		else {
			print("There are "+tickets.length+" "+type+" tickets.");
		}
		var body = "<ul>\n";
		for (var k=0; k<tickets.length; k++) {
		    var ticket = tickets[k];
		
		    // Example: http://sourceforge.net/p/geneontology/ontology-requests/10193/ cilium/flagellum 
		    body += '<li>';
		    body += '<a href="http://sourceforge.net/p/'+oname+'/ontology-requests/'+ticket.ticket_num+'/">' + ticket.ticket_num + '</a>';
		    body += " ";
		    body += makeHtmlSave(ticket.summary);
		    body += '</li>\n';
		}
		body += "</ul>"
		print(body);
	}else {
		print("<p>There have been no "+type+" tickets.</p>");
	}
}

/**
 * Save guard against problematic text.<br>
 * Escape HTML reserved characters and remove non-ASCII symbols.
 */
function makeHtmlSave(s) {
	// rempve all non-ascii symbols with a '?'
	s = s.replace(/[^A-Za-z 0-9 \.,\?""!@#\$%\^&\*\(\)-_=\+;:<>\/\\\|\}\{\[\]`~]*/g, '') ;
	// escape html specific characters 
	return s.replace(/&/g, '&amp;')
    	.replace(/"/g, '&quot;')
    	.replace(/>/g, '&gt;')
    	.replace(/</g, '&lt;');
}

function getNewTickets(oname, trackername, range) {
	//EXAMPLE = "http://sourceforge.net/rest/p/geneontology/ontology-requests/search?q=created_date_dt:[2013-07-30T00:00:00Z%20TO%202013-07-30T23:59:59Z]";
	return getTickets(oname, trackername, range, 'created_date_dt');
}

function getUpdatedTickets(oname, trackername, range) {
	//EXAMPLE = "http://sourceforge.net/rest/p/geneontology/ontology-requests/search?q=mod_date_dt:[2013-07-30T00:00:00Z%20TO%202013-07-30T23:59:59Z]";
	return getTickets(oname, trackername, range, 'mod_date_dt');
}

/**
 * Get the tickets. Warning: This method uses system.exit(-1) to exit the
 * program in case of an error.
 * 
 * TODO: handle pagination, maybe add a retry count
 */
function getTickets(oname, trackername, range, type) {
	var url = 'http://sourceforge.net/rest/p/'+oname+'/'+trackername+'/search?q='+type+':['+range+']';
	//console.log(url);
	var exchange = httpclient.get(url);
	var payload = exchange.content;
	if (exchange.status === 200) {
		//console.log(payload);
		var resultObj = JSON.parse(payload);
		return resultObj.tickets;
	}
	else {
		print('Http error status code: '+exchange.status+' for request url: '+url);
		system.exit('-1');
	}
}

// call the main method; ringo specific
if (require.main == module.id) {
	main(system.args);
}