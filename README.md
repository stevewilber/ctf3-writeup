Stripe ran it’s third CTF challenge last week with a theme of distributed systems. I found the problems well designed and very interesting. The Stripe team did a ton of work creating the challenges, the test harness and the infrastructure for the contest. They did an amazing job and I’m very thankful to Stripe for the very fun and educational experience! To top it off they even hosted a really awesome meetup at the Stripe office in San Francisco to wrap up the contest.

The contest consisted of 5 levels of progamming challenges. For each level, the participant was provided with some code that was either broken or too slow. It was up to you to either fix the existing code or rewrite from scratch. When you pushed your code, it was run against a random test case and compared against a reference solution. On some levels the goal was to beat the reference solution, for others you needed to hit a predefined score.

My goals were to complete the contest and to work as idiomatically as possible in the provided languages.

My <a href="https://github.com/stevewilber/ctf3-writeup" target="_blank">code</a> and <a href="https://stripe-ctf.com/achievements/swilber" target="_blank">CTF3 profile</a>.
<h2>Level 0</h2>
<strong>Language:</strong> Ruby

<strong>Challenge:</strong> Spell check a file against a provided dictionary. The starter code read the dictionary into an array and then iterated over the array looking for a match for each word.

<strong>My Solution:</strong> Read the dictionary into a hash set for O(1) lookups.

<strong>Better Solution:</strong> Precompute the lookup data structure at build time. Use a Bloom filter for even faster lookups. This can yield false positives, but if this happens you can push again.
<h2>Level 1</h2>
<strong>Language:</strong> Bash

<strong>Challenge:</strong> Mine a coin in a new cryptocurrrency called Gitcoin. To mine a coin, you needed to commit to a git repo with a SHA-1 that was lexicographically smaller than the provided difficulty string. If someone found one before you, the tree would change so your commit was no longer valid and you would have to start over. A bot was running on the server mining against your repo, so the goal was to mine a coin before the bot.

The bash script provided that called out to ‘git hash-object’ to get the SHA-1 of the commit. This was run in a loop until a match was found.

<strong>My Solution: </strong>A bash solution was probably never going to be fast enough so this one required a rewrite in another language. I rewrote the provided script in Ruby and used ‘digest/sha1' to calculate the SHA-1. This was fast enough to pass the challenge.

<strong>Better Solution: </strong>Write a multi-threaded miner in a faster language and run on GPUs. Instead of computing the hash of the whole commit string, precompute the static part of the string and add the hash of the nonce.

<strong>Bonus Round: </strong>After beating this challenge, a bonus level was unlocked. Instead of mining against a bot, all of the players shared a repo and mined against each other. I got in early enough that I was able to mine 1 coin, but the competition quickly escalated to miners running in GPUs doing an incredible number of hashes/sec.
<h2>Level 2</h2>
<strong>Language:</strong> Javascript/Node.js

<strong>Challenge: </strong>Build a proxy to protect an API from a DDOS attack. The test starts up two backend servers and a proxy. The code provided for the proxy forwards all requests and only utilizes one server. The requests include the IP and we know that legitmate users are sending just a few requests whereas malicious users are throwing as much traffic as possible at the server.

<strong>My Solution: </strong>The first change was make use of both servers by randomly forwarding the request to one of the nodes. A naive solution of blocking any user that makes more than 5 requests during the test was enough to pass this challenge.

<strong>Better Solution: </strong>The test runs for 20 seconds during which time malicious users are hitting the server as fast as possible. We could spend the first few seconds allowing all traffic through, analyze the traffic and block the IPs that are sending too many requests. Could also throttle users based on the number of requests that they send.
<h2>Level 3</h2>
<strong>Language:</strong> Scala

<strong>Challenge: </strong>Given a string, return the file name and line number for all matches (including substrings) in a set of files. There is a master server which accepts the request and 3 servers to execute the search. Each server has 500MB of memory and 4 minutes to index the files. The initial code walks through the files and calls String.contains() on every line.

<strong>My Solution: </strong>I found this challenge frustrating but really fun. I had never worked with Scala before and while the syntax felt foreign, I found some of the language features like pattern matching and futures to be really cool.

Substring matching was not mentioned in the readme so my solution ended up being pretty sub-optimal. I started by trying to build a reverse index of all of the search files in a hashmap; the key was a word and value was a list of file ids and line numbers. This turned out to use too much memory and failed to pass the indexing stage. I was able to optimize this enough to pass by making two modifications. We knew that searches would come from the provided ‘words’ file, so I only indexed words contained in that file. I also split the indexing across the 3 nodes and merged the results.

<strong>Better Solution: </strong>Since we need to support substring matching, my solution required iterating over all of the keys in the inverted index. A better data structure for this would have been a trie or a suffix array.
<h2>Level 4</h2>
<strong>Language:</strong> Go

<strong>Challenge: </strong>Create an SQL database cluster. The provided solution used SQLLite and replicated queries to the other nodes. The test harness threw queries at all of the nodes and verified they were executed in the correct order and that data was consistent across the nodes. While doing this it also simulated various network failures: severed links between nodes, slow connections and temporary downtime of a node.

<strong>My Solution: </strong>I was able to get a passing score by turning off replication and proxying all queries to the master. I heard that this hack/loophole was later closed, but I didn’t get a chance to come back to it and do a more proper solution.

<strong>Better Solution: </strong>There is a consensus algorithm called Raft and a library for Go called goraft. Also the provided code shelled out to sqlite3 to write the DB, this could be sped up significantly by using sqlite bindings and keeping the database in memory.
