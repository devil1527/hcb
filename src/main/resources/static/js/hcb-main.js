// HCB Main Client Script
function changeProductQty(productId, amount) {
    const input = document.getElementById('qty-' + productId);
    if (!input) return;
    const maxStock = parseInt(input.getAttribute('data-stock') || input.getAttribute('data-max')) || 999;
    let currentVal = parseInt(input.value) || 1;
    if (amount > 0 && currentVal >= maxStock) {
        alert('Limited stock available! Only ' + maxStock + ' available in stock.');
        return;
    }
    currentVal += amount;
    if (currentVal < 1) currentVal = 1;
    if (currentVal > maxStock) currentVal = maxStock;
    input.value = currentVal;
}
