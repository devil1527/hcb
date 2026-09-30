// HCB Client-Side Shopping Cart (LocalStorage backed)
const CART_STORAGE_KEY = 'hcb_cart';

function getCart() {
    try {
        const stored = localStorage.getItem(CART_STORAGE_KEY);
        return stored ? JSON.parse(stored) : [];
    } catch (e) {
        console.error('Failed to parse cart', e);
        return [];
    }
}

function saveCart(cart) {
    try {
        localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
    } catch (e) {
        console.error('Failed to save cart', e);
    }
}

function handleAddToCartBtn(btn) {
    if (!btn || btn.disabled) return;
    const roleMeta = document.querySelector('meta[name="hcb-user-role"]');
    if (roleMeta && roleMeta.getAttribute('content') === 'ADMIN') {
        alert('Administrator Notice: Admin accounts manage the store and cannot add items to cart or place customer orders. To place customer orders, please use a customer account.');
        return;
    }
    const productId = parseInt(btn.getAttribute('data-id')) || 0;
    const name = btn.getAttribute('data-name') || '';
    const price = parseFloat(btn.getAttribute('data-price')) || 0;
    const image = btn.getAttribute('data-image') || 'banner.png';
    const maxStock = parseInt(btn.getAttribute('data-stock')) || 999;
    addToCart(productId, name, price, 'qty-' + productId, image, maxStock);
}

function addToCart(productId, name, price, qtyInputId, imageFilename, maxStock = 999) {
    const qtyInput = document.getElementById(qtyInputId);
    let qty = qtyInput ? (parseInt(qtyInput.value) || 1) : 1;
    let cart = getCart();

    const existingIndex = cart.findIndex(item => item.productId === productId);
    let currentInCart = existingIndex > -1 ? cart[existingIndex].qty : 0;

    if (currentInCart + qty > maxStock) {
        let allowedToAdd = Math.max(0, maxStock - currentInCart);
        if (allowedToAdd === 0) {
            alert("Limited stock available! You already have all " + maxStock + " available items in your cart.");
            openCart();
            return;
        } else {
            alert("Limited stock available! Only " + maxStock + " available in total. Added remaining " + allowedToAdd + " to your cart.");
            qty = allowedToAdd;
        }
    }

    if (existingIndex > -1) {
        cart[existingIndex].qty += qty;
        cart[existingIndex].maxStock = maxStock;
    } else {
        cart.push({
            productId: productId,
            name: name,
            price: parseFloat(price),
            qty: qty,
            image: imageFilename || 'banner.png',
            maxStock: maxStock
        });
    }

    saveCart(cart);
    updateCartUI();
    openCart();
}

function updateCartItemQty(index, change) {
    let cart = getCart();
    if (cart[index]) {
        if (change > 0 && cart[index].maxStock && cart[index].qty >= cart[index].maxStock) {
            alert("Limited stock available! Only " + cart[index].maxStock + " available in stock.");
            return;
        }
        cart[index].qty += change;
        if (cart[index].qty <= 0) {
            cart.splice(index, 1);
        }
        saveCart(cart);
        updateCartUI();
    }
}

function removeFromCart(index) {
    let cart = getCart();
    cart.splice(index, 1);
    saveCart(cart);
    updateCartUI();
}

function clearCart() {
    localStorage.removeItem(CART_STORAGE_KEY);
    updateCartUI();
}

function toggleCart() {
    const modal = document.getElementById('cartModal');
    const overlay = document.getElementById('cartOverlay');
    if (modal && overlay) {
        modal.classList.toggle('open');
        overlay.classList.toggle('open');
    }
}

function openCart() {
    const modal = document.getElementById('cartModal');
    const overlay = document.getElementById('cartOverlay');
    if (modal && overlay) {
        modal.classList.add('open');
        overlay.classList.add('open');
    }
}

function closeCart() {
    const modal = document.getElementById('cartModal');
    const overlay = document.getElementById('cartOverlay');
    if (modal && overlay) {
        modal.classList.remove('open');
        overlay.classList.remove('open');
    }
}

function updateCartUI() {
    const cart = getCart();
    const countSpans = document.querySelectorAll('.cart-count-badge');
    const itemsContainer = document.getElementById('cartItemsContainer');
    const checkoutSection = document.getElementById('checkoutSection');
    const subtotalSpan = document.getElementById('cartSubtotal');
    const shippingDisplay = document.getElementById('cartShippingDisplay');
    const totalPriceSpan = document.getElementById('cartTotalPrice');
    const freeShippingNoticeBox = document.getElementById('freeShippingNoticeBox');

    let totalCount = 0;
    let subtotal = 0;

    cart.forEach(item => {
        totalCount += item.qty;
        subtotal += item.price * item.qty;
    });

    countSpans.forEach(el => el.innerText = totalCount);

    if (!itemsContainer) return;

    if (cart.length === 0) {
        itemsContainer.innerHTML = '<p style="text-align: center; color: var(--text-muted); margin-top: 2rem;">Your cart is empty.</p>';
        if (checkoutSection) checkoutSection.style.display = 'none';
        return;
    }

    let html = '';
    cart.forEach((item, index) => {
        html += `
            <div class="cart-item">
                <div class="cart-item-details">
                    <h4>${escapeHtml(item.name)}</h4>
                    <p>₹${item.price.toFixed(2)} each</p>
                </div>
                <div class="cart-qty-controls">
                    <button type="button" class="cart-qty-btn" onclick="updateCartItemQty(${index}, -1)">-</button>
                    <span style="font-weight: 600; min-width: 20px; text-align: center; font-size: 0.9rem;">${item.qty}</span>
                    <button type="button" class="cart-qty-btn" onclick="updateCartItemQty(${index}, 1)">+</button>
                    <button type="button" class="remove-item" onclick="removeFromCart(${index})" title="Remove"><i class="fas fa-trash"></i></button>
                </div>
            </div>
        `;
    });

    // Shipping calculation (free shipping threshold >= 499)
    let shippingFee = subtotal >= 499 ? 0 : 100;
    let finalTotal = subtotal + shippingFee;

    if (shippingDisplay && freeShippingNoticeBox) {
        if (subtotal >= 499) {
            shippingDisplay.innerHTML = '<del style="color: #999;">₹100</del> <span style="color: var(--success-green); font-weight: 700; margin-left: 5px;">₹0 (FREE)</span>';
            freeShippingNoticeBox.innerHTML = '🎉 <strong>Congratulations!</strong> You have unlocked Free Delivery.';
            freeShippingNoticeBox.style.color = 'var(--success-green)';
        } else {
            let amountNeeded = (499 - subtotal).toFixed(2);
            shippingDisplay.innerText = '₹100';
            freeShippingNoticeBox.innerHTML = `🚀 Add <strong>₹${amountNeeded}</strong> more to get <strong>Free Delivery</strong>!`;
            freeShippingNoticeBox.style.color = 'var(--choco-dark)';
        }
    }

    itemsContainer.innerHTML = html;
    if (subtotalSpan) subtotalSpan.innerText = '₹' + subtotal.toFixed(2);
    if (totalPriceSpan) totalPriceSpan.innerText = '₹' + finalTotal.toFixed(2);
    if (checkoutSection) checkoutSection.style.display = 'block';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener("DOMContentLoaded", function() {
    updateCartUI();
});
