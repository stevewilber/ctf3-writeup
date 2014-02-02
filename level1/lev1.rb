#!/usr/bin/env ruby

require 'digest/sha1'
require 'Open3'

while true
	`git fetch`
	`git reset --hard origin/master`

	`echo "user-ni0rfzoo: 1" >> LEDGER.txt` 
	`git add LEDGER.txt`

	difficulty = File.open("difficulty.txt", "rb").read.strip

	time = `date +%s`.strip;

	tree = `git write-tree`.strip
	parent = `git rev-parse HEAD`.strip
	author = "lvl1-p5bnnbku <me@example.com> #{time} +0000"
	committer = "lvl1-p5bnnbku <me@example.com> #{time} +0000"
	body = "tree #{tree}\nparent #{parent}\nauthor #{author}\ncommitter #{committer}\n\nGive me a Gitcoin\n\n"
	# body = tree + parent + author + committer + "Give me a Gitcoin\n\n"

	hash = difficulty
	counter = 0
	header = ""

	while (hash <=> difficulty) >= 0
		counter += 1;
		content = body + counter.to_s
		header = "commit #{content.length}\0"
		hash = Digest::SHA1.hexdigest(header + content)
	end

	puts hash
	puts header 
	puts content

	puts "git hash-object"
	# `git hash-object -t commit --stdin -w <<< "#{content}"`
	stdin, stdout, stderr = Open3.popen3('git hash-object -t commit --stdin -w')
	stdin.print(content)
	stdin.close
	puts stdout.gets
	puts "git reset"
	`git reset --hard #{hash}`
	puts "git push"
	`git push origin master`
end
