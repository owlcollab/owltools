var Parser = require('ringo/args').Parser;
var httpclient = require('ringo/httpclient');
var system = require('system');

// TODO add option to print to a separate file.
function main(args) {
	var script = args.shift();
	var parser = new Parser(system.args);
	parser.addOption('u', 'username', 'USERNAME', 'github org or user name, defaults to "geneontology"');
	parser.addOption('p', 'project', 'PROJECT', 'github project name, defaults to "go-ontology"');
	parser.addOption('r', 'range', 'RANGE', 'date range, defaults to one day');
	parser.addOption('h', 'help', null, 'Display help');

	var options = parser.parse(args);
	if (options.help) {
		print(parser.help());
		system.exit('-1');
	}
	
	var username = 'geneontology';
	if (options.username !== undefined) {
		username = options.username;
	}

	var projectname = 'go-ontology';
	if (options.project !== undefined) {
		projectname = options.project;
	}
	
	var durationInDays = 1;
	if (options.range !== undefined) {
		durationInDays = parseInt(options.range);
	}
	
	var today = new Date();
	var yesterday = new Date(new Date().setDate(new Date().getDate()-durationInDays));
	// yyyy-mm-dd quick hack
	var range = yesterday.toISOString().substring(0, 10);
	//console.log(range);
	
	var newTickets = getNewTickets(username, projectname, range);
	var modTickets = getUpdatedTickets(username, projectname, range);
	
	print("<h2>Summary for tickets from "+yesterday.toISOString()+" to "+today.toISOString()+"</h2>");
	var ids = {}; // track already used ids
	printNewTickets(newTickets, username, projectname, ids);
	printUpdatedTickets(modTickets, username, projectname, ids);
	
}

function printNewTickets(tickets, username, projectname, ids) {
	printTickets(tickets, username, 'new', 'New', projectname, ids);
}

function printUpdatedTickets(tickets, username, projectname, ids) {
	printTickets(tickets, username, 'updated', 'Updated', projectname, ids);
}

function printTickets(tickets, username, type, typeUpperCase, projectname, ids) {
	print("<h3>"+typeUpperCase+" Tickets</h3>");
	if (tickets !== undefined && tickets.length > 0) {

		var ticketCount = 0;
		var body = "<ul>\n";
		for (var k=0; k<tickets.length; k++) {
			var ticket = tickets[k];
			var alreadyUsed = ids[ticket.number];
			if (alreadyUsed === true) {
				// skip already used tickets
				continue;
			}
			body += '<li>';
			body += '<a href="'+ticket.html_url+'">' + ticket.number + '</a>';
			body += " ";
			body += makeHtmlSave(ticket.title);
			body += '</li>\n';

			ticketCount += 1;
			ids[ticket.number] = true;
		}
		if (tickets.length > 0) {
			if (tickets.length === 1) {
				print("There is one "+type+" ticket.");
			}
			else {
				print("There are "+ticketCount+" "+type+" tickets.");
			}
			body += "</ul>"
			print(body);
		}
		else {
			print("<p>There have been no "+type+" tickets.</p>");
		}
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

function getNewTickets(username, projectname, range) {
	// Example: https://api.github.com/search/issues?q=repo:geneontology/go-ontology+created:=>2015-08-05&type=Issues&per_page=100
	return getTickets(username, projectname, range, 'created:=>');
}

function getUpdatedTickets(username, projectname, range) {
	// Example: https://api.github.com/search/issues?q=repo:geneontology/go-ontology+updated:=>2015-08-05&type=Issues&per_page=100
	return getTickets(username, projectname, range, 'updated:=>');
}

/**
 * Get the tickets. Warning: This method uses system.exit(-1) to exit the
 * program in case of an error.
 * 
 * TODO: handle pagination, maybe add a retry count
 */
function getTickets(username, projectname, range, type) {
	var url = 'https://api.github.com/search/issues?q=repo:'+username+'/'+projectname+'+'+type+range+'&type=Issues&per_page=100';
	console.log(url);
	var exchange = httpclient.get(url);
	var payload = exchange.content;
	if (exchange.status === 200) {
		//console.log(payload);
		var resultObj = JSON.parse(payload);
		/*
		 * TODO check:
		 * "total_count": 5,
		 * "incomplete_results": false,
		 *  "items": [{
		 *    number,
		 *    title,
		 *    html_url
		 *  }]
		 */
		return resultObj.items;
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
