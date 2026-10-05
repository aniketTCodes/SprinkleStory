import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { CakeSlice, Package, Tags, Truck, ShoppingBag, ClipboardList, Layers, ArrowLeftRight, Users, ArrowUpRight, Sparkles } from 'lucide-react'
import ResourcePage from './ResourcePage'

const working = [
  { path: '/products', label: 'Products', icon: Package },
  { path: '/categories', label: 'Categories', icon: Tags },
  { path: '/suppliers', label: 'Suppliers', icon: Truck },
]
const upcoming = [
  { path: '/sales', label: 'Sales', icon: ShoppingBag, description: 'Checkout, receipts, and payment recording for every sweet sale.' },
  { path: '/purchase-orders', label: 'Purchase orders', icon: ClipboardList, description: 'Keep supplier orders and incoming deliveries in one place.' },
  { path: '/inventory-lots', label: 'Inventory lots', icon: Layers, description: 'Track batch quantities, pricing, and best-before dates.' },
  { path: '/stock-movements', label: 'Stock movements', icon: ArrowLeftRight, description: 'Follow stock history and record inventory adjustments.' },
  { path: '/users', label: 'Users & access', icon: Users, description: 'Manage staff accounts, roles, and account status.' },
]

function Placeholder({ page }: { page: typeof upcoming[number] }) {
  const Icon = page.icon
  return <div className="page">
    <div className="eyebrow">ROOM TO GROW</div>
    <header className="page-header"><div><h1>{page.label}</h1><p>{page.description}</p></div><span className="badge coming">Coming soon</span></header>
    <section className="placeholder panel">
      <div className="placeholder-icon"><Icon size={36} strokeWidth={1.5} /></div>
      <span className="eyebrow">A LITTLE SOMETHING IN THE WORKS</span>
      <h2>{page.label}, all in good time.</h2>
      <p>This feature is not available yet. Once it is ready, you’ll find it right here.</p>
      <NavLink className="button primary" to="/products">Back to products <ArrowUpRight size={17} /></NavLink>
    </section>
  </div>
}

export default function App() {
  return <div className="app">
    <a className="skip-link" href="#main">Skip to content</a>
    <aside className="sidebar">
      <NavLink to="/products" className="brand" aria-label="Sprinkle Story home"><span className="brand-mark"><CakeSlice size={27} /></span><span>sprinkle story<small>YOUR SWEET SHOP, IN ORDER</small></span></NavLink>
      <div className="shop-label"><span className="shop-dot" />Store workspace</div>
      <nav aria-label="Main navigation">
        <div className="nav-label">MANAGE YOUR STORE</div>
        {working.map(({ path, label, icon: Icon }) => <NavLink key={path} to={path} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}><Icon size={19} /><span>{label}</span></NavLink>)}
        <div className="nav-label upcoming-label">UP NEXT</div>
        {upcoming.map(({ path, label, icon: Icon }) => <NavLink key={path} to={path} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}><Icon size={18} /><span>{label}</span><span className="soon-dot" aria-label="Coming soon" /></NavLink>)}
      </nav>
      <div className="sidebar-note"><Sparkles size={21} /><p>A little order.<br /><strong>A lot more sweetness.</strong></p><span>Sprinkle Story · Inventory</span></div>
    </aside>
    <div className="workspace">
      <div className="topbar"><span>Store management <span className="topbar-divider">/</span> Workspace</span><span className="topbar-brand"><CakeSlice size={16} /> Made for your sweet shop</span></div>
      <main id="main" tabIndex={-1}>
        <Routes>
          <Route path="/" element={<Navigate to="/products" replace />} />
          <Route path="/products" element={<ResourcePage key="products" resource="products" />} />
          <Route path="/categories" element={<ResourcePage key="categories" resource="categories" />} />
          <Route path="/suppliers" element={<ResourcePage key="suppliers" resource="suppliers" />} />
          {upcoming.map(page => <Route key={page.path} path={page.path} element={<Placeholder page={page} />} />)}
          <Route path="*" element={<div className="page"><h1>Page not found</h1><NavLink to="/products" className="button primary">Back to products</NavLink></div>} />
        </Routes>
      </main>
      <footer>A little order for your sweet shop.<span>Sprinkle Story</span></footer>
    </div>
  </div>
}

