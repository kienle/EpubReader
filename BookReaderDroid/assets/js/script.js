var $book;
var pageWidth = 800;
var pageHeight = 400;
var padding = 20;
var bookReader;

$(document).ready(function (){
    $book = $('#book');
    
    pageWidth = $(window).width();
    console.log("pageWidth: " + pageWidth);
    bookReader = new BookReader($book, pageWidth, pageHeight, padding);
    
    //bookReader.gotoPage(20);
    
    $(window).bind('click', function(event) {
        bookReader.nextPage();
    });
    
    $(window).bind('touchstart', function(event) {
        //bookReader.nextPage();
    });
});

BookReader = function($book, pageWidth, pageHeight, padding) {
    this.$book = $book;
    this.pageWidth = pageWidth;
    this.pageHeight = pageHeight;
    this.padding = padding;
    this.columnWidth = this.pageWidth - 2 * this.padding;
    this.columnHeight = pageHeight;

    this.init();
};

BookReader.prototype.init = function() {
    this.currentPage = 0;
	
    this.$book.css('width', this.pageWidth);
    this.$book.css('height', this.pageHeight);
    this.$book.css('position', 'absolute');
    //this.$book.css('overflow', 'hidden');
    this.$book.css('-webkit-column-width', this.columnWidth);
    this.$book.css('-webkit-column-gap', 2 * this.padding);
    this.$book.css('min-width', 2 * this.pageWidth);
    this.$book.css('padding', this.padding);
    //this.$book.css('-webkit-column-rule', '1em solid #000');
    this.$book.css('text-align', 'justify');
    //this.$book.css('border', '1px solid #000');

    //console.log(this.$book.css('-webkit-column-count'));
    //this.pagesTotal = Math.floor(this.$book[0].scrollWidth / (this.pageWidth + this.padding));
    this.pagesTotal = this.getColumnCount();
    //console.log('this.$book[0].scrollWidth: ' + this.$book[0].scrollWidth);
    console.log('this.pagesTotal: ' + this.pagesTotal);
    
    //$endChapter = $("#chapter-end");
    //$endChapter.css("display", "inline");
    //console.log('pos ' + $endChapter.position().left);
};

BookReader.prototype.nextPage = function() {	
    this.gotoPage(this.currentPage + 1);
};

BookReader.prototype.prevPage = function() {	
    this.gotoPage(this.currentPage - 1);
};

BookReader.prototype.gotoPage = function(page) {
    this.currentPage = Math.max(0, Math.min(page, this.pagesTotal - 1));

    this.$book.animate({left: -this.currentPage * (this.pageWidth + this.padding) - this.padding / 2}, 300);
};

BookReader.prototype.getColumnCount = function () {
    var $endChapter = $("#chapter-end");
    
    $endChapter.css("display", "inline");
    
    var pos = $endChapter.position();
    pos.top -= this.padding;
    if (pos.top < 0) {
        pos.top = 0;
    }
    
    //console.log('pos.left ' + pos.left);
    //console.log('this.$book[0].scrollWidth: ' + this.$book[0].scrollWidth);
    var count = Math.floor(pos.left / (this.pageWidth + this.padding));// + Math.floor(pos.top / this.columnHeight);
    if (pos.top % this.columnHeight > 20) {
        count++;
    }
    
    //var count = Math.floor(pos.left / this.pageWidth);
    
    //if (pos.left % this.pageWidth > 20) count++;
    //console.log(pos.left / (this.pageWidth + this.padding));
    
    //$endChapter.css('background-color', '#000');
    $endChapter.css("display", "none");
    
    return count;
};