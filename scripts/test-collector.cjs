const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const {parseHTML} = require(process.env.DOM_TEST_LIBRARY || 'linkedom');
const source = fs.readFileSync('app/src/main/assets/collect-offers.js', 'utf8');
function collect(platform, html, url) {
  const {document} = parseHTML(`<html><head><title>Fixture only</title></head><body>${html}</body></html>`);
  return JSON.parse(vm.runInNewContext(source.replace('__PLATFORM__', JSON.stringify(platform)), {document, location:new URL(url), URL, Set, Array, String, Number, JSON}));
}
let checks = 0;
function test(name, action) { action(); checks++; console.log(`PASS ${name}`); }
test('Taobao card price cannot come from a different item', () => {
  const r = collect('TAOBAO', `<section><div class="goods-item"><a href="https://item.taobao.com/item.htm?id=10001"><img alt="FixturePhone 16 黑色"/><h3 class="title">FixturePhone 16 黑色</h3></a><del class="old-price">¥999</del><div class="price">¥<span>199</span><span>.90</span></div><div class="shop-name">测试淘宝店</div></div><div class="goods-item"><a href="https://item.taobao.com/item.htm?id=10002"><img alt="另一款测试手机"/><h3 class="title">另一款测试手机</h3></a><div class="price">¥299.00</div><div class="shop-name">另一家测试店</div></div></section>`, 'https://s.m.taobao.com/search?q=FixturePhone');
  assert.equal(r.items.length, 2); assert.equal(r.items[0].price, '199.90'); assert.equal(r.items[0].shop, '测试淘宝店'); assert.equal(r.items[0].image,''); assert.equal(r.items[1].price, '299.00');
});
test('Tmall result and actual shop are read', () => {
  const r=collect('TMALL', '<div class="product-item"><a href="https://detail.tmall.com/item.htm?id=20001"><img alt="FixturePhone 天猫测试"/><b class="title">FixturePhone 天猫测试</b></a><span class="Price">￥209.10起</span><span class="shopName">测试天猫店</span></div>', 'https://list.tmall.com/search_product.htm?q=FixturePhone');
  assert.equal(r.items[0].price,'209.10');assert.equal(r.items[0].starting,true);assert.equal(r.items[0].shop,'测试天猫店');
});
test('JD card with explicit SKU supports split prices', () => {
  const r=collect('JD','<li sku="30001"><img alt="FixturePhone 京东测试"/><h3 class="ware-name">FixturePhone 京东测试</h3><strong class="price">¥<span>189</span><span>.80</span></strong><span class="shop-name">测试京东店</span></li>','https://so.m.jd.com/ware/search.action?keyword=FixturePhone');
  assert.equal(r.items[0].price,'189.80');assert.equal(r.items[0].url,'https://item.m.jd.com/product/30001.html');
});
test('PDD current public CSS-module card does not invent a shop or product URL', () => {
  const r=collect('PDD','<div class="_2EYTUfrB"><img src="https://img.pddpic.com/test.png"/><div class="_1YcMjCeq">FixturePhone 拼多多测试</div><div class="_2psv9tFx">¥179.00</div></div>','https://mobile.yangkeduo.com/search_result.html?search_key=FixturePhone');
  assert.equal(r.items[0].price,'179.00');assert.equal(r.items[0].shop,'');assert.equal(r.items[0].url,'');
});
test('Coupon, installment and missing-price content does not become product prices', () => {
  const r=collect('JD','<div class="goods-item"><a href="https://item.m.jd.com/product/40001.html"><img alt="FixturePhone 未定价测试"/><h3 class="title">FixturePhone 未定价测试</h3></a><div class="price">每期¥9.90</div><span class="coupon-price">优惠券20</span></div>','https://so.m.jd.com/');
  assert.equal(r.items.length,0);
});
test('JSON-LD Product price and seller are extracted', () => {
  const r=collect('JD','<script type="application/ld+json">'+JSON.stringify({'@type':'ItemList',itemListElement:[{item:{'@type':'Product',name:'FixturePhone 结构化测试',url:'https://item.jd.com/50001.html',offers:{price:'150.25',priceCurrency:'CNY',seller:{name:'结构化测试店铺'}}}}]})+'</script>','https://so.m.jd.com/');
  assert.equal(r.items[0].price,'150.25');assert.equal(r.items[0].shop,'结构化测试店铺');
});
test('Login and verification are reported without fabricated offers', () => {
  const a=collect('JD','<div>请先登录</div><input type="password"/>','https://passport.m.jd.com/login');assert.equal(a.login,true);assert.equal(a.items.length,0);
  const b=collect('PDD','<div>请完成验证 滑块验证</div>','https://mobile.yangkeduo.com/');assert.equal(b.verification,true);assert.equal(b.items.length,0);
});
test('Parent containers cannot mix prices across different products', () => {
  const r=collect('JD','<div class="goods-item"><a href="https://item.jd.com/60001.html"><img alt="第一款测试商品"/></a><a href="https://item.jd.com/60002.html"><img alt="第二款测试商品"/></a><h3 class="title">共享商品列表标题</h3><div class="price">99</div></div>','https://so.m.jd.com/');assert.equal(r.items.length,0);
});
test('Clearly empty pages remain empty', () => {
  const r=collect('TAOBAO','<p>没有找到相关商品</p>','https://s.m.taobao.com/');assert.equal(r.empty,true);assert.equal(r.items.length,0);
});
test('Hidden template prices are not searchable offers', () => {
  const r=collect('JD','<div hidden class="goods-item"><a href="https://item.jd.com/70001.html"><img alt="隐藏测试商品"/><h3 class="title">隐藏测试商品</h3></a><div class="price">1.00</div></div>','https://so.m.jd.com/');assert.equal(r.items.length,0);
});
console.log(`PASS: ${checks} collector DOM checks (fixtures only, no live platform login).`);
