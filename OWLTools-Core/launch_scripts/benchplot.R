plottype <- "l"
#reasoners = names[3:8]
#reasoners <- c("owlapi_factpp", "owlapi_pellet", "oboedit_lpr", "oboedit_rbr", "obdsql", "blipkit")
reasoners <- c("owlapi_factpp","oboedit_rbr", "pellet")

#jpeg("test.jpg")
fr <- read.table("stats.txt", sep="\t", header=TRUE, row.names=1)
stats <- as.matrix(fr)

names <- names(fr)
rnames <- rownames(fr)

ylim <- 300
#ylim <- max(fr)

xs <- as.vector(fr[,1]) # every row of 1st column: rank of ont


ys <- as.vector(fr[,3]) # 1st set of data
#ys2 <- as.vector(fr[,4])
ys2 <- as.vector(fr[,5])

nclrs <- length(reasoners)

s <- 3
e <- 10

bp <- barplot(t(as.matrix(fr[reasoners])), main="Reasoning completion time", ylab= "Time", las=2, xlab="",cex.axis=0.5,
   beside=TRUE, col=rainbow(nclrs))

legend("topleft", cex=2, reasoners,
   bty="n", fill=rainbow(nclrs))


plot(xs, xlab="ont size", ylab="time", main="reasoning completion time", ylim=c(0,ylim), pch=15, col="blue")
#textxy(xs,labs=names)
#axis(1, labels=names)
#axis(1, at=1:5, 

clrs <- c("red","green","blue","yellow","grey")
n <- 0
for (i in reasoners) {
  n <- n+1
  points(xs, as.vector(fr[,i]), pch=n, col=clrs[n], type=plottype)
  #points(xs, as.vector(fr[,i]), pch=n, col=clrs[n])
}
legend('topleft', col=clrs, pch=c(1:6), legend=reasoners)

#plot(xs,ys, xlab="ont size", ylab="time", main="reasoning completion time", ylim=c(0,1800), pch=15, col="blue", type="l")
# fit a line to the points
#myline.fit <- lm(ys ~ xs)

# get information about the fit
#summary(myline.fit)

# draw the fit line on the plot
#abline(myline.fit) 

#points(xs, ys2, pch=16, col="green", type="l")
#points(xs, as.vector(fr[,6]), pch=17, col="red", type="l")
#plot(stats,ys, xlab="x axis", ylab="y axis", main="reasoners", pch=15, col="blue")


#dev.off()
