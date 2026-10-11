
const $ = id => document.getElementById(id);

let products = [];
let customers = [];
let cart = new Map();
let toastTimer;

const money = n => new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0
}).format(n || 0);

async function api(path, options = {}) {
    const r = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });

    let d = {};
    try {
        d = await r.json();
    } catch {}

    if (!r.ok) {
        throw new Error(d.error || 'No fue posible completar la solicitud');
    }

    return d;
}

async function loadAll() {
    try {
        [products, customers] = await Promise.all([
            api('/api/productos'),
            api('/api/clientes')
        ]);

        // Elimina del carrito productos que ya no existan.
        for (const id of cart.keys()) {
            if (!products.some(p => p.id === id)) {
                cart.delete(id);
            }
        }

        // Evita mantener cantidades superiores al stock actual.
        for (const [id, cantidad] of cart) {
            const p = products.find(x => x.id === id);

            if (p && cantidad > p.stock) {
                if (p.stock > 0) {
                    cart.set(id, p.stock);
                } else {
                    cart.delete(id);
                }
            }
        }

        renderProducts();
        renderCustomers();
        renderCart();
        await loadOrders();
    } catch (e) {
        alert(e.message);
    }
}

/* Notificación temporal */
function showToast(message) {
    let toast = $('cartToast');

    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'cartToast';
        toast.className = 'cart-toast';
        toast.setAttribute('role', 'status');
        toast.setAttribute('aria-live', 'polite');
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.classList.add('show');

    clearTimeout(toastTimer);

    toastTimer = setTimeout(() => {
        toast.classList.remove('show');
    }, 2200);
}

/* Menú de productos */
function renderProducts() {
    $('products').innerHTML = products.map((p, i) => {
        const cantidad = cart.get(p.id) || 0;
        const restantes = Math.max(0, p.stock - cantidad);

        return `
            <article class="product">
                <div class="emoji">${['☕', '🥐', '🍵', '🥛'][i % 4]}</div>

                <h3>${esc(p.nombre)}</h3>

                <p class="muted">
                    ${esc(p.descripcion || 'Preparado al momento.')}
                </p>

                <div class="price-row">
                    <div>
                        <div class="price">${money(p.precio)}</div>
                        <div class="stock">
                            ${restantes > 0
                                ? `${restantes} disponibles`
                                : 'Sin unidades disponibles'}
                        </div>
                    </div>

                    <button
                        class="add"
                        ${restantes <= 0 ? 'disabled' : ''}
                        onclick="addCart('${p.id}')">
                        + Añadir
                    </button>
                </div>
            </article>
        `;
    }).join('');
}

/* Clientes */
function renderCustomers() {
    const old = $('customer').value;

    $('customer').innerHTML =
        '<option value="">Seleccione un cliente</option>' +
        customers.map(c => `
            <option value="${c.id}">
                ${esc(c.nombre)} · ${esc(c.nivel)} · ${c.devPoints} pts
            </option>
        `).join('');

    if (customers.some(c => c.id === old)) {
        $('customer').value = old;
    }
}

/* Agregar una unidad */
function addCart(id) {
    const p = products.find(x => x.id === id);

    if (!p) return;

    const cantidad = cart.get(id) || 0;

    if (cantidad >= p.stock) {
        showToast('No quedan más unidades disponibles.');
        return;
    }

    cart.set(id, cantidad + 1);

    renderProducts();
    renderCart();

    showToast(`¡${p.nombre} añadido al carrito!`);
}

/* Mostrar carrito, cantidades, stock restante y subtotal */
function renderCart() {
    if (!cart.size) {
        $('cart').innerHTML =
            '<p class="muted">Aún no hay productos seleccionados.</p>';

        renderProducts();
        return;
    }

    let total = 0;

    $('cart').innerHTML = [...cart].map(([id, cantidad]) => {
        const p = products.find(x => x.id === id);

        if (!p) return '';

        total += p.precio * cantidad;

        const restantes = Math.max(0, p.stock - cantidad);

        return `
            <div class="cart-row cart-item">
                <div class="cart-item-info">
                    <strong>${esc(p.nombre)}</strong>

                    <small class="cart-stock">
                        ${restantes} ${restantes === 1
                            ? 'unidad restante'
                            : 'unidades restantes'}
                    </small>
                </div>

                <div class="cart-item-actions">
                    <button
                        type="button"
                        class="quantity-btn"
                        aria-label="Restar una unidad de ${esc(p.nombre)}"
                        onclick="removeCart('${p.id}')">
                        −
                    </button>

                    <strong class="quantity-value">${cantidad}</strong>

                    <button
                        type="button"
                        class="quantity-btn"
                        aria-label="Sumar una unidad de ${esc(p.nombre)}"
                        ${cantidad >= p.stock ? 'disabled' : ''}
                        onclick="addCart('${p.id}')">
                        +
                    </button>

                    <strong class="cart-line-total">
                        ${money(p.precio * cantidad)}
                    </strong>
                </div>
            </div>
        `;
    }).join('') + `
        <div class="cart-row">
            <b>Subtotal estimado</b>
            <b>${money(total)}</b>
        </div>
    `;
}

/* Restar una unidad; al llegar a cero se quita del carrito */
function removeCart(id) {
    const cantidad = cart.get(id) || 0;

    if (cantidad <= 1) {
        cart.delete(id);
    } else {
        cart.set(id, cantidad - 1);
    }

    renderProducts();
    renderCart();
}

/* Crear cliente */
$('clientForm').addEventListener('submit', async e => {
    e.preventDefault();

    $('clientMessage').textContent = 'Creando perfil…';

    try {
        const c = await api('/api/clientes', {
            method: 'POST',
            body: JSON.stringify({
                nombre: $('clientName').value,
                correo: $('clientEmail').value
            })
        });

        $('clientMessage').textContent =
            `Perfil creado. Bienvenido, ${c.nombre}.`;

        $('clientForm').reset();

        await loadAll();

        $('customer').value = c.id;
    } catch (err) {
        $('clientMessage').textContent = err.message;
    }
});

/* Crear pedido: se conserva el flujo original */
$('orderForm').addEventListener('submit', async e => {
    e.preventDefault();

    if (!cart.size) {
        $('orderMessage').textContent =
            'Añada al menos un producto.';
        return;
    }

    try {
        const o = await api('/api/pedidos', {
            method: 'POST',
            body: JSON.stringify({
                clienteId: $('customer').value,
                modalidad: $('modalidad').value,
                puntosRedimir: Number($('points').value) || 0,
                items: [...cart].map(([productoId, cantidad]) => ({
                    productoId,
                    cantidad
                }))
            })
        });

        cart.clear();
        $('points').value = 0;

        $('orderMessage').textContent =
            `Pedido ${o.id} creado. Total: ${money(o.total)}. ` +
            `Descuento nivel: ${money(o.descuentoNivel)}; ` +
            `puntos: ${money(o.descuentoPuntos)}.`;

        await loadAll();
    } catch (err) {
        $('orderMessage').textContent = err.message;
    }
});

/* Historial y estados de pedidos: se conserva el flujo original */
async function loadOrders() {
    const orders = await api('/api/pedidos');

    const states = {
        PENDIENTE_PAGO: 'Pendiente de pago',
        EN_PREPARACION: 'En preparación',
        LISTO: 'Listo',
        ENTREGADO: 'Entregado'
    };

    $('orders').innerHTML = orders.length
        ? orders.slice().reverse().map(o => {
            const c = customers.find(x => x.id === o.clienteId);

            const next = {
                PENDIENTE_PAGO: 'EN_PREPARACION',
                EN_PREPARACION: 'LISTO',
                LISTO: 'ENTREGADO'
            }[o.estado];

            return `
                <article class="order">
                    <div>
                        <strong>${esc(o.id)}</strong>
                        <small>
                            ${esc(c?.nombre || o.clienteId)} ·
                            ${o.modalidad === 'LLEVAR'
                                ? 'Para llevar'
                                : 'En mesa'}
                        </small>
                    </div>

                    <div>
                        <strong>${money(o.total)}</strong>
                        <small>
                            ${o.items.map(i =>
                                `${i.cantidad}× ${esc(i.nombreProducto)}`
                            ).join(', ')}
                        </small>
                    </div>

                    <div>
                        <span class="state">
                            ${states[o.estado] || esc(o.estado)}
                        </span>

                        <small style="display:block;margin-top:5px">
                            ${new Date(o.fecha).toLocaleString('es-CO')}
                        </small>
                    </div>

                    ${next
                        ? `<button onclick="advance('${o.id}','${next}')">
                               Avanzar →
                           </button>`
                        : '<small>Compra completada ✓</small>'}
                </article>
            `;
        }).join('')
        : '<p class="muted">Todavía no hay pedidos.</p>';
}

async function advance(id, estado) {
    try {
        await api(`/api/pedidos/${id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ estado })
        });

        await loadAll();
    } catch (e) {
        alert(e.message);
    }
}

function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[c]);
}

loadAll();
